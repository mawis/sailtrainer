/*
 *  Copyright 2014 Matthias Wimmer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.wimmerinformatik.sbfs.data;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import eu.wimmerinformatik.sbfs.R;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.SimpleCursorAdapter;

public class Repository extends SQLiteOpenHelper {
	private static final String QUESTION = "question";
	private static final String QUESTION_QUERY = "_id=?";
	private static final String ANSWER = "answer";
	private static final String ANSWER_QUERY =
		"question_id=? AND order_index=?";
	private static final String CORRECT_ANSWER = "0";
	private static final String FIRST_INCORRECT = "1";
	private static final String SECOND_INCORRECT = "2";
	private static final String THIRD_INCORRECT = "3";
	private static final int NUMBER_LEVELS = 5;

	private final Context context;
	private int answerIdSeq;
	private SQLiteDatabase database;
	private final String done;

	public Repository(final Context context) {
		super(context, "topics", null, 5);
		done = context.getString(R.string.done);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// create databases
		db.beginTransaction();
		try {
			db.execSQL("CREATE TABLE topic (_id INT NOT NULL PRIMARY KEY, order_index INT NOT NULL UNIQUE, name TEXT NOT NULL)");
			db.execSQL("CREATE TABLE question (_id INT NOT NULL PRIMARY KEY, topic_id INT NOT NULL REFERENCES topic(_id) ON DELETE CASCADE, reference TEXT, question TEXT NOT NULL, level INT NOT NULL, next_time INT NOT NULL)");
			db.execSQL("CREATE TABLE answer (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(_id) ON DELETE CASCADE, order_index INT NOT NULL, answer TEXT NOT NULL)");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		// fill with data
		try {
			final List<Topic> topics = new LinkedList<Topic>();
			final List<Question> questions = new LinkedList<Question>();
			final XmlResourceParser xmlResourceParser = context.getResources().getXml(R.xml.sbfsfragen);
			int eventType = xmlResourceParser.getEventType();
			Topic currentTopic = null;
			Question currentQuestion = null;
			boolean expectingAnswer = false;
			boolean expectingQuestion = false;
			int index = 0;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					final String tagName = xmlResourceParser.getName();
					if ("topic".equals(tagName)) {
						currentTopic = new Topic();
						currentTopic.setId(xmlResourceParser.getAttributeIntValue(null, "id", 0));
						currentTopic.setIndex(index++);
						currentTopic.setName(xmlResourceParser.getAttributeValue(null, "name"));
					} else if ("question".equals(tagName)) {
						currentQuestion = new Question();
						currentQuestion.setId(xmlResourceParser.getAttributeIntValue(null, "id", 0));
						currentQuestion.setReference(xmlResourceParser.getAttributeValue(null, "reference"));
						currentQuestion.setNextTime(new Date());
						currentQuestion.setTopicId(currentTopic.getId());
					} else if ("text".equals(tagName)) {
						expectingQuestion = true;
					} else if ("correct".equals(tagName)) {
						expectingAnswer = true;
					} else if ("incorrect".equals(tagName)) {
						expectingAnswer = true;
					}
					break;
				case XmlPullParser.TEXT:
					if (expectingQuestion) {
						currentQuestion.setQuestionText(xmlResourceParser.getText());
						expectingQuestion = false;
					}
					if (expectingAnswer) {
						currentQuestion.getAnswers().add(xmlResourceParser.getText());
						expectingAnswer = false;
					}
				case XmlPullParser.END_TAG:
					final String endTagName = xmlResourceParser.getName();
					if ("topic".equals(endTagName)) {
						topics.add(currentTopic);
						currentTopic = null;
					} else if ("question".equals(endTagName)) {
						questions.add(currentQuestion);
						currentQuestion = null;
					}
					break;
				}
				xmlResourceParser.next();
				eventType = xmlResourceParser.getEventType();
			}
			xmlResourceParser.close();

			db.beginTransaction();
			try {
				for (final Topic topic : topics) {
					save(db, topic);
				}
				for (final Question question : questions) {
					save(db, question);
				}
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	public QuestionSelection selectQuestion(final int topicId) {
		final QuestionSelection result = new QuestionSelection();
		final List<Integer> possibleQuestions = new LinkedList<Integer>();
		final long now = new Date().getTime();

		int questionCount = 0;
		int openQuestions = 0;
		int maxProgress = 0;
		int currentProgress = 0;
		long soonestNextTime = 0;

		final Cursor c = getDb().query("question", new String[]{"_id", "level", "next_time"}, "topic_id=?", new String[]{Integer.toString(topicId)}, null, null, null, null);
		try {
			c.moveToNext();
			while (!c.isAfterLast()) {
				final int questionId = c.getInt(0);
				final int level = c.getInt(1);
				final long nextTime = c.getLong(2);

				questionCount++;
				maxProgress += NUMBER_LEVELS;
				currentProgress += level;
				if (level < NUMBER_LEVELS) {
					openQuestions++;

					if (nextTime > now) {
						if (soonestNextTime == 0 || soonestNextTime > nextTime) {
							soonestNextTime = nextTime;
						}
					} else {
						possibleQuestions.add(questionId);
					}
				}

				c.moveToNext();
			}

		} finally {
			c.close();
		}

		result.setTotalQuestions(questionCount);
		result.setMaxProgress(maxProgress);
		result.setCurrentProgress(currentProgress);
		result.setOpenQuestions(openQuestions);
		result.setFinished(possibleQuestions.isEmpty() && soonestNextTime == 0);
		if (!possibleQuestions.isEmpty()) {
			Random rand = new Random();
			result.setSelectedQuestion(possibleQuestions.get(rand.nextInt(possibleQuestions.size())));
		} else if (soonestNextTime > 0) {
			result.setNextQuestion(new Date(soonestNextTime));
		}

		return result;
	}

	public Question getQuestion(final int questionId) {
		final Question question = new Question();

		final Cursor c = getDb().query("question", new String[]{"_id", "topic_id", "reference", "question", "level", "next_time"}, "_id=?", new String[]{Integer.toString(questionId)}, null, null, null, null);
		try {
			c.moveToNext();
			if (c.isAfterLast()) {
				return null;
			}
			question.setId(c.getInt(0));
			question.setTopicId(c.getInt(1));
			question.setReference(c.getString(2));
			question.setQuestionText(c.getString(3));
			question.setLevel(c.getInt(4));
			question.setNextTime(new Date(c.getLong(5)));
		} finally {
			c.close();
		}

		// _id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(id) ON DELETE CASCADE, order_index INT NOT NULL, answer TEXT
		final Cursor answer = getDb().query("answer", new String[]{"answer"}, "question_id=?", new String[]{Integer.toString(questionId)}, null, null, "order_index");
		try {
			answer.moveToNext();
			while (!answer.isAfterLast()) {
				question.getAnswers().add(answer.getString(0));
				answer.moveToNext();
			}
		} finally {
			answer.close();
		}

		return question;
	}

	public Topic getTopic(final int topicId) {
		final Topic topic = new Topic();

		final Cursor c = getDb().query("topic", new String[]{"_id", "order_index", "name"}, "_id=?", new String[]{Integer.toString(topicId)}, null, null, null);
		try {
			c.moveToNext();
			if (c.isAfterLast()) {
				return null;
			}
			topic.setId(c.getInt(0));
			topic.setIndex(c.getInt(1));
			topic.setName(c.getString(2));
		} finally {
			c.close();
		}

		return topic;
	}

	public TopicStats getTopicStat(final int topicId) {
		final TopicStats stats = new TopicStats();
		stats.setLevels(NUMBER_LEVELS);
		stats.setQuestionsAtLevel(new int[NUMBER_LEVELS+1]);

		int currentProgress = 0;
		int maxProgress = 0;
		int questionCount = 0;

		final Cursor c = getDb().query("question", new String[]{"_id", "level"}, "topic_id=?", new String[]{Integer.toString(topicId)}, null, null, null, null);
		try {
			c.moveToNext();
			while (!c.isAfterLast()) {
				questionCount++;
				currentProgress += c.getInt(1);
				maxProgress += NUMBER_LEVELS;
				stats.getQuestionsAtLevel()[c.getInt(1)]++;
				c.moveToNext();
			}
		} finally {
			c.close();
		}

		stats.setCurrentProgress(currentProgress);
		stats.setMaxProgress(maxProgress);
		stats.setQuestionCount(questionCount);

		return stats;
	}

	public void answeredCorrect(final int questionId) {
		final Question question = getQuestion(questionId);
		final int newLevel = question.getLevel() + 1;

		updateAnswered(questionId, newLevel);
	}

	public void answeredIncorrect(final int questionId) {
		final Question question = getQuestion(questionId);
		final int newLevel = question.getLevel() <= 0 ? 0 : question.getLevel() - 1;

		updateAnswered(questionId, newLevel);
	}

	public void continueNow(final int topicId) {
		final ContentValues updates = new ContentValues();
		updates.put("next_time", new Date().getTime());
		getDb().update("question", updates, "topic_id=?", new String[]{Integer.toString(topicId)});
	}

	public void resetTopic(final int topicId) {
		final ContentValues updates = new ContentValues();
		updates.put("next_time", new Date().getTime());
		updates.put("level", 0L);
		getDb().update("question", updates, "topic_id=?", new String[]{Integer.toString(topicId)});		
	}

	public void setTopicsInSimpleCursorAdapter(final SimpleCursorAdapter adapter) {
		final Cursor c = getTopicsCursor(getDb());
		adapter.changeCursor(c);
	}

	public Cursor getTopicsCursor(final SQLiteDatabase db) {
		final Cursor cursor = db.rawQuery("SELECT t._id AS _id, t.order_index AS order_index, t.name AS name, CASE WHEN MIN(level) >= " + NUMBER_LEVELS + " THEN ? ELSE SUM(CASE WHEN level < " + NUMBER_LEVELS +" THEN 1 ELSE 0 END) END AS status, MIN(CASE WHEN level >= " + NUMBER_LEVELS + " THEN NULL ELSE next_time END) AS next_question FROM topic t LEFT JOIN question q ON q.topic_id = t._id GROUP BY t._id, t.order_index, t.name ORDER BY t.order_index", new String[]{done});
		return cursor;
	}

	private void updateAnswered(final int questionId, final int newLevel) {
		final long newNextTime = new Date().getTime() + waitingTimeOnLevel(newLevel);

		final ContentValues updates = new ContentValues();
		updates.put("level", newLevel);
		updates.put("next_time", newNextTime);

		getDb().update("question", updates, "_id=?", new String[]{Integer.toString(questionId)});
	}

	private long waitingTimeOnLevel(final int level) {
		return level <= 0 ? 15000L :
			level == 1 ? 60000L :
			level == 2 ? 30*60000L :
			level == 3 ? 86400000L :
			level == 4 ? 3*86400000L :
			0;
	}

	private void save(final SQLiteDatabase db, Topic currentTopic) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put("_id", currentTopic.getId());
		contentValues.put("order_index", currentTopic.getIndex());
		contentValues.put("name", currentTopic.getName());
		db.insert("topic", null, contentValues);
	}

	private void save(final SQLiteDatabase db, final Question question) {
		final ContentValues contentValues = new ContentValues();
		contentValues.put("_id", question.getId());
		contentValues.put("topic_id", question.getTopicId());
		contentValues.put("reference", question.getReference());
		contentValues.put("question", question.getQuestionText());
		contentValues.put("level", 0);
		contentValues.put("next_time", question.getNextTime().getTime());
		db.insert("question", null, contentValues);

		int answerIndex = 0;
		for (final String answer : question.getAnswers()) {
			contentValues.clear();
			contentValues.put("_id", ++answerIdSeq);
			contentValues.put("question_id", question.getId());
			contentValues.put("order_index", answerIndex++);
			contentValues.put("answer", answer);
			db.insert("answer", null, contentValues);
		}
	}

	private SQLiteDatabase getDb() {
		if (database == null) {
			database = this.getWritableDatabase();
		}
		return database;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion <= 2) {
			final ContentValues updates = new ContentValues();
			updates.put("question", "Welches Funkzeugnis ist mindestens erforderlich, um mit einer Seefunkstelle auf einem Sportfahrzeug am Weltweiten Seenot- und Sicherheitsfunksystem (GMDSS) im Seegebiet A3 teilnehmen zu können?");
			db.update("question", updates, "_id=?", new String[]{"4408"});
		}
		if (oldVersion <= 3) {
			final ContentValues updates = new ContentValues();
			updates.put("answer", "Sportboote ohne Antriebsmaschine oder solche mit einer größten nicht überschreitbaren Nutzleistung von 11,03 Kilowatt (15 PS) oder weniger.");
			db.update("answer", updates, "question_id=? AND order_index=?", new String[]{"9664", "0"});
			updates.clear();
			updates.put("answer", "Sportboote mit Antriebsmaschine mit einer größeren Nutzleistung als 11,03 Kilowatt (15 PS).");
			db.update("answer", updates, "question_id=? AND order_index=?", new String[]{"9664", "2"});
		}
		if (oldVersion <= 4) {
			updateQuestion(
					db,
					"9589",
					"Was ist zu tun, wenn vor Antritt der Fahrt nicht feststeht, wer Schiffsführer ist?");
			updateAnswer(
					db,
					"9589",
					CORRECT_ANSWER,
					"Der verantwortliche Schiffsführer muss bestimmt werden.");
			updateAnswer(
					db,
					"9589",
					FIRST_INCORRECT,
					"Der verantwortliche Schiffsführer muss gewählt werden.");
			updateAnswer(
					db,
					"9589",
					SECOND_INCORRECT,
					"Ein Inhaber eines Sportbootführerscheins muss die "
					+ "Fahrzeugführung übernehmen.");
			updateAnswer(
					db,
					"9589",
					THIRD_INCORRECT,
					"Ein Inhaber eines Sportbootführerscheins muss die "
					+ "Verantwortung übernehmen.");

			updateAnswer(
					db,
					"9674",
					FIRST_INCORRECT,
					"Die Kollisionsverhütungsregeln (KVR), die "
					+ "Seeschifffahrtsstraßen-Ordnung (SeeSchStrO) und die "
					+ "Sportbootführerscheinverordnung.");

			updateAnswer(
					db,
					"9742",
					FIRST_INCORRECT,
					"Die Nachrichten für Seefahrer (NfS), herausgegeben vom "
					+ "Bundesamt für Seeschifffahrt und Hydrographie, sowie "
					+ "die Bekanntmachungen für Seefahrer (BfS) der örtlich "
					+ "zuständigen Wasserstraßen- und Schifffahrtsämter, die "
					+ "auf alle Veränderungen hinsichtlich Betonnung, Wracks "
					+ "und Untiefen sowie auf die Schifffahrt betreffende "
					+ "Maßnahmen und Ereignisse hinweisen.");
			updateAnswer(
					db,
					"9742",
					SECOND_INCORRECT,
					"Die nautische Veröffentlichung „Sicherheit auf dem "
					+ "Wasser, herausgegeben durch das Bundesministerium für "
					+ "Verkehr und digitale Infrastruktur (BMVI), mit "
					+ "wichtigen Regeln und Tipps für Wassersportler.");

			updateAnswer(
					db,
					"9743",
					SECOND_INCORRECT,
					"Ergänzende Vorschriften für den NOK in der "
					+ "Seeschifffahrtsstraßen-Ordnung sowie in der "
					+ "Sportbootführerscheinverordnung.");

			updateAnswer(
					db,
					"9814",
					SECOND_INCORRECT,
					"Befahrensregeln beachten sowie Wasserschutzpolizei und "
					+ "Wasserstraßen- und Schifffahrtsamt informieren.");
		}
	}

	private void updateQuestion(
			final SQLiteDatabase db,
			final String id,
			final String text) {

		final ContentValues update = new ContentValues();
		update.put(QUESTION, text);
		db.update(QUESTION, update, QUESTION_QUERY, new String[]{id});
	}

	private void updateAnswer(
			final SQLiteDatabase db,
			final String question,
			final String answerNo,
			final String text) {

		final ContentValues upd = new ContentValues();
		upd.put(ANSWER, text);
		db.update(ANSWER, upd, ANSWER_QUERY, new String[]{question, answerNo});
	}
}
