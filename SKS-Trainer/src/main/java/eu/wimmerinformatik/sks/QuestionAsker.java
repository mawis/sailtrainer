/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
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

package eu.wimmerinformatik.sks;

import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import eu.wimmerinformatik.sks.data.Question;
import eu.wimmerinformatik.sks.data.QuestionSelection;
import eu.wimmerinformatik.sks.data.Repository;
import eu.wimmerinformatik.sks.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * @author matthias
 *
 */
public class QuestionAsker extends Activity {
	private Repository repository;
	private int currentQuestion;
	private int maxProgress;
	private int currentProgress;
	private int topicId;
	private int correctChoice;
	private Drawable defaultBackground;
	private boolean showingCorrectAnswer;
	private Date nextTime;
	private Timer waitTimer;
	private boolean showingStandardView;
	private boolean replaceNNBSP = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		cancelTimer();
		
		repository.close();
		repository = null;
		defaultBackground = null;
		nextTime = null;
	}
	
	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(getClass().getName() + ".showingCorrectAnswer", showingCorrectAnswer);
		outState.putInt(getClass().getName() + ".currentQuestion", currentQuestion);
		outState.putInt(getClass().getName() + ".maxProgress", maxProgress);
		outState.putInt(getClass().getName() + ".currentProgress", currentProgress);
		outState.putLong(getClass().getName() + ".topic", topicId);
		if (nextTime != null) {
			outState.putLong(getClass().getName() + ".nextTime", nextTime.getTime());
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        repository = new Repository(this);
        
        showStandardView();
        
        if (savedInstanceState != null) {
        	topicId = (int) savedInstanceState.getLong(getClass().getName()+".topic");
        	currentQuestion = savedInstanceState.getInt(getClass().getName()+".currentQuestion");
        	final long nextTimeLong = savedInstanceState.getLong(getClass().getName()+".nextTime");
        	nextTime = nextTimeLong > 0L ? new Date(nextTimeLong) : null;
        	showingCorrectAnswer = savedInstanceState.getBoolean(getClass().getName()+".showingCorrectAnswer");
        	maxProgress = savedInstanceState.getInt(getClass().getName() + ".maxProgress");
        	currentProgress = savedInstanceState.getInt(getClass().getName() + ".currentProgress");
        	
        	showQuestion();
        } else {
        	topicId = (int) getIntent().getExtras().getLong(getClass().getName()+".topic");
        	nextQuestion();
        }
        

	}
	
	/**
	 * Populate the options menu.
	 * 
	 * @param menu the menu to populate
	 * @return always true
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trainingmenu, menu);
		return true;
	}
	
	/**
	 * Handle option menu selections.
	 * 
	 * @param item the Item the user selected
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// handle item selection
		switch (item.getItemId()) {
		case R.id.resetTopic:
			askRestartTopic();
			return true;
		case R.id.statistics:
			final Intent intent = new Intent(this, StatisticsActivity.class);
			intent.putExtra(StatisticsActivity.class.getName() + ".topic", topicId);
			startActivity(intent);
			return true;
		case R.id.help:
			final StringBuilder uri = new StringBuilder();
			uri.append("http://sportboot.mobi/trainer/segeln/sks/app/help?question=");
			uri.append(currentQuestion);
			uri.append("&topic=");
			uri.append(topicId);
			uri.append("&view=QuestionAsker");
			final Intent intend = new Intent(Intent.ACTION_VIEW);
			intend.setData(Uri.parse(uri.toString()));
			startActivity(intend);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Restarts the topic after asking for confirmation.
	 */
	private void askRestartTopic() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.warningReset);
		builder.setCancelable(true);
		builder.setPositiveButton(R.string.resetOkay, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				restartTopic();
			}
		});
		builder.setNegativeButton(R.string.resetCancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void showStandardView() {
        setContentView(R.layout.question_asker);
        showingStandardView = true;
        showingCorrectAnswer = false;
	
		final Button contButton = (Button) findViewById(R.id.showAnswerButton);
        contButton.setOnClickListener(new View.OnClickListener() {

			// @Override
			public void onClick(View v) {
				showingCorrectAnswer = true;
				showQuestion();
			}
        });		
	}
	
	private void nextQuestion() {
		if (!showingStandardView) {
			showStandardView();
		}
		
		if (correctChoice != 0 && defaultBackground != null) {
			final RadioButton correctButton = (RadioButton) findViewById(correctChoice);
			correctButton.setBackgroundDrawable(defaultBackground);
		}
		
		final QuestionSelection nextQuestion = repository.selectQuestion(topicId);
		
		// any question?
		final int selectedQuestion = nextQuestion.getSelectedQuestion();
		if (selectedQuestion != 0) {
			currentQuestion = selectedQuestion;
			maxProgress = nextQuestion.getMaxProgress();
			currentProgress = nextQuestion.getCurrentProgress();
			nextTime = null;
			showQuestion();
			return;
		}
		
		nextTime = nextQuestion.getNextQuestion();
		if (nextTime != null) {
			showQuestion();
			return;
		}
		
		showingStandardView = false;
		setContentView(R.layout.no_more_questions_finished);

		final Button restartTopicButton = (Button) findViewById(R.id.restartTopic);
		restartTopicButton.setOnClickListener(new View.OnClickListener() {
			//@Override
			public void onClick(View v) {
				restartTopic();
			}
			
		});
		return;
	}
	
	private void restartTopic() {
		repository.resetTopic(topicId);
		nextQuestion();
	}
	
	private void showQuestion() {
		if (nextTime != null) {
			showingStandardView = false;
			setContentView(R.layout.no_more_questions_wait);
			
			final TextView nextTimeText = (TextView) findViewById(R.id.nextTimeText);
			if (nextTime.getTime() - new Date().getTime() < 64800000L) {
				nextTimeText.setText(DateFormat.getTimeInstance().format(nextTime));
			} else {
				nextTimeText.setText(DateFormat.getDateTimeInstance().format(nextTime));
			}
			showNextQuestionAt(nextTime);
			
			final Button resetWaitButton = (Button) findViewById(R.id.resetWait);
			resetWaitButton.setOnClickListener(new View.OnClickListener() {
				//@Override
				public void onClick(View v) {
					cancelTimer();
					repository.continueNow(topicId);
					nextQuestion();
					return;
				}
				
			});
			return;
		}
		
		if (showingCorrectAnswer) {
			showingStandardView = false;
			setContentView(R.layout.show_answer);
			
			final Question question = repository.getQuestion(currentQuestion);
			
			final TextView levelText = (TextView) findViewById(R.id.answerLevelText);
			levelText.setText(question.getLevel() == 0 ? getString(R.string.firstPass) :
				question.getLevel() == 1 ? getString(R.string.secondPass) : "");
			
			final TextView textView = (TextView) findViewById(R.id.answerTextViewFrage);
			textView.setText(safeText(question.getQuestionText()));
			
			final TextView answerView = (TextView) findViewById(R.id.answerText);
			answerView.setText(safeText(question.getAnswer()));
			
			final ProgressBar progressBar = (ProgressBar) findViewById(R.id.answerProgressBar);
			progressBar.setMax(maxProgress);
			progressBar.setProgress(currentProgress);
			
			final Button correctButton = (Button) findViewById(R.id.buttonCorrect);
			correctButton.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					repository.answeredCorrect(currentQuestion);
					nextQuestion();
				}
			});
			
			final Button incorrectButton = (Button) findViewById(R.id.buttonIncorrect);
			incorrectButton.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					repository.answeredIncorrect(currentQuestion);
					nextQuestion();
				}
			});
			
			// remove previous question image if any
			final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.answerLinearLayout);
			for (int i = linearLayout.getChildCount() - 1; i >= 0; i--) {
				final View childAtIndex = linearLayout.getChildAt(i);
				if (childAtIndex instanceof ImageView) {
					linearLayout.removeViewAt(i);
				}
			}
			
			final ImageView answerImage = getAnswerImage();
			if (answerImage != null) {
				linearLayout.addView(answerImage, 4);
			}
			
			final ImageView questionImage = getQuestionImage();
			if (questionImage != null) {
				linearLayout.addView(questionImage, 2);
			}
			
			return;
		}
		
		final Question question = repository.getQuestion(currentQuestion);
		
		final TextView levelText = (TextView) findViewById(R.id.levelText);
		levelText.setText(question.getLevel() == 0 ? getString(R.string.firstPass) :
			question.getLevel() == 1 ? getString(R.string.secondPass) : "");

		final TextView textView = (TextView) findViewById(R.id.textViewFrage);
        
		textView.setText(safeText(question.getQuestionText()));
		
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBar.setMax(maxProgress);
		progressBar.setProgress(currentProgress);
		
		// remove previous question image if any
		final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout1);
		for (int i = 0; i < linearLayout.getChildCount(); i++) {
			final View childAtIndex = linearLayout.getChildAt(i);
			if (childAtIndex instanceof ImageView) {
				linearLayout.removeViewAt(i);
				break;
			}
		}
		
		final ImageView questionImage = getQuestionImage();
		if (questionImage != null) {
			linearLayout.addView(questionImage, 2);
		}
	}
	
	private ImageView getAnswerImage() {
		int imageResourceId = -1;
		switch (currentQuestion) {
		case 1313:
			imageResourceId = R.drawable.sm_answer41;
			break;
		case 1350:
			imageResourceId = R.drawable.sm_answer78;
			break;
		case 1351:
			imageResourceId = R.drawable.sm_answer79;
			break;
		case 1376:
			imageResourceId = R.drawable.sm_answer104;
			break;
		case 8553:
			imageResourceId = R.drawable.smii_answer64;
			break;
		case 8554:
			imageResourceId = R.drawable.smii_answer65;
			break;
		default:
			return null;
		}
		
		final ImageView image = new ImageView(this);
		image.setImageResource(imageResourceId);
		return image;
	}
	
	private ImageView getQuestionImage() {
		int imageResourceId = -1;
		switch (currentQuestion) {
		case 1025:
			imageResourceId = R.drawable.nav84;
			break;
		case 1079:
			imageResourceId = R.drawable.sr19;
			break;
		case 1081:
			imageResourceId = R.drawable.sr21;
			break;
		case 1082:
			imageResourceId = R.drawable.sr22;
			break;
		case 1083:
			imageResourceId = R.drawable.sr23;
			break;
		case 1109:
			imageResourceId = R.drawable.sr49;
			break;
		case 1111:
			imageResourceId = R.drawable.sr51;
			break;
		case 1137:
			imageResourceId = R.drawable.sr77;
			break;
		case 1138:
			imageResourceId = R.drawable.sr78;
			break;
		case 1142:
			imageResourceId = R.drawable.sr82;
			break;
		case 1204:
			imageResourceId = R.drawable.w33;
			break;
		case 1205:
			imageResourceId = R.drawable.w34;
			break;
		case 1350:
			imageResourceId = R.drawable.sm78;
			break;
		case 1351:
			imageResourceId = R.drawable.sm79;
			break;
		case 1376:
			imageResourceId = R.drawable.sm104;
			break;
		case 8553:
			imageResourceId = R.drawable.smii64;
			break;
		case 8554:
			imageResourceId = R.drawable.smii65;
			break;
		default:
			return null;
		}
		
		final ImageView image = new ImageView(this);
		image.setImageResource(imageResourceId);
		return image;
	}
	
	private void showNextQuestionAt(final Date when) {
		scheduleTask(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					//@Override
					public void run() {
						nextQuestion();
					}
				});
			}
			
		}, when);
	}
	
	private synchronized void scheduleTask(final TimerTask task, final Date when) {
		cancelTimer();
		waitTimer = new Timer("waitNextQuestion", true);
		waitTimer.schedule(task, when);
	}
	
	private synchronized void cancelTimer() {
		if (waitTimer != null) {
			waitTimer.cancel();
			waitTimer = null;
		}
	}
	
	private String safeText(final String source) {
		return replaceNNBSP && source != null ? source.replace('\u202f', '\u00a0') : source;
	}
}
