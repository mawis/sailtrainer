package eu.wimmerinformatik.trainer.data;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.SimpleCursorAdapter;

import eu.wimmerinformatik.trainer.R;

public class Repository extends SQLiteOpenHelper {
	private final Context context;
	private int answerIdSeq;
	private SQLiteDatabase database;

	public Repository(final Context context) {
		super(context, "topics", null, 1);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// create databases
		db.beginTransaction();
		try {
			db.execSQL("CREATE TABLE topic (_id INT NOT NULL PRIMARY KEY, order_index INT NOT NULL UNIQUE, name TEXT NOT NULL)");
			db.execSQL("CREATE TABLE question (_id INT NOT NULL PRIMARY KEY, topic_id INT NOT NULL REFERENCES topic(id) ON DELETE CASCADE, reference TEXT, question TEXT NOT NULL, level INT NOT NULL, next_time INT NOT NULL)");
			db.execSQL("CREATE TABLE answer (_id INT NOT NULL PRIMARY KEY, question_id INT NOT NULL REFERENCES question(id) ON DELETE CASCADE, order_index INT NOT NULL, answer TEXT NOT NULL)");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
				
		// fill with data
		try {
			final List<Topic> topics = new LinkedList<Topic>();
			final List<Question> questions = new LinkedList<Question>();
			final XmlResourceParser xmlResourceParser = context.getResources().getXml(R.xml.ubifragen);
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
	
	public void setTopicsInSimpleCursorAdapter(final SimpleCursorAdapter adapter) {
		final Cursor c = getTopicsCursor(getDb());
		adapter.changeCursor(c);
	}
	
	public Cursor getTopicsCursor(final SQLiteDatabase db) {
		final Cursor cursor = db.query("topic", new String[]{"_id","order_index","name"}, null, null, null, null, "order_index");
		return cursor;
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
		// TODO Auto-generated method stub

	}

}
