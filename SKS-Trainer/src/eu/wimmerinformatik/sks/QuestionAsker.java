/**
 * 
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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
	private int topicId;
	private int correctChoice;
	private Drawable defaultBackground;
	private boolean showingCorrectAnswer;
	private Date nextTime;
	private Timer waitTimer;
	private boolean showingStandardView;
	
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
        	
        	showQuestion();
        } else {
        	topicId = (int) getIntent().getExtras().getLong(getClass().getName()+".topic");
        	nextQuestion();
        }
        

	}
	
	private void showStandardView() {
        setContentView(R.layout.question_asker);
        showingStandardView = true;
        showingCorrectAnswer = false;

		ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar1);
		progress.setMax(2);
	
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
				repository.resetTopic(topicId);
				nextQuestion();
			}
			
		});
		return;
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
			
			final TextView textView = (TextView) findViewById(R.id.answerTextViewFrage);
			textView.setText(question.getQuestionText());
			
			final TextView answerView = (TextView) findViewById(R.id.answerText);
			answerView.setText(question.getAnswer());
			
			final ProgressBar progressBar = (ProgressBar) findViewById(R.id.answerProgressBar);
			progressBar.setMax(2);
			progressBar.setProgress(question.getLevel());
			
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
			
			return;
		}
		
		final Question question = repository.getQuestion(currentQuestion);

		final TextView textView = (TextView) findViewById(R.id.textViewFrage);
        
		textView.setText(question.getQuestionText());
		
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBar.setProgress(question.getLevel());
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
}
