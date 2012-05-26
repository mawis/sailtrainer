/**
 * 
 */
package eu.wimmerinformatik.sbfb;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import eu.wimmerinformatik.sbfb.data.Question;
import eu.wimmerinformatik.sbfb.data.QuestionSelection;
import eu.wimmerinformatik.sbfb.data.Repository;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author matthias
 *
 */
public class QuestionAsker extends Activity {
	private Repository repository;
	private int currentQuestion;
	private int topicId;
	private int correctChoice;
	private Random rand = new Random();
	private Drawable defaultBackground;
	private int correctMarkBackground;
	private List<Integer> order;
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
		rand = null;
		defaultBackground = null;
		order = null;
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
		
		if (order != null) {
			final StringBuilder orderString = new StringBuilder();
			for (int i = 0; i < order.size(); i++) {
				if (i > 0) {
					orderString.append(',');
				}
				orderString.append(order.get(i));
			}
			outState.putString(getClass().getName() + ".order", orderString.toString());
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
        	
        	final String orderString = savedInstanceState.getString(getClass().getName()+".order");
        	if (orderString != null) {
        		final String[] orderArray = orderString.split(",");
        		order = new LinkedList<Integer>();
        		for (int i = 0; i < orderArray.length; i++) {
        			order.add(Integer.parseInt(orderArray[i]));
        		}
        	}
        	
        	showQuestion();
        } else {
        	topicId = (int) getIntent().getExtras().getLong(getClass().getName()+".topic");
        	nextQuestion();
        }
        

	}
	
	private void showStandardView() {
        setContentView(R.layout.question_asker);
        showingStandardView = true;

		ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar1);
		progress.setMax(5);
	
		final Button contButton = (Button) findViewById(R.id.button1);
        contButton.setOnClickListener(new View.OnClickListener() {

			// @Override
			public void onClick(View v) {

				// find what has been selected
				final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
				
				if (showingCorrectAnswer) {
					showingCorrectAnswer = false;
					radioGroup.setEnabled(true);
					nextQuestion();
					return;
				}
				
				int selectedButton = radioGroup.getCheckedRadioButtonId();
				if (selectedButton == correctChoice) {
					Toast.makeText(QuestionAsker.this, getString(R.string.right), Toast.LENGTH_SHORT).show();
					
					repository.answeredCorrect(currentQuestion);
					
					nextQuestion();
					
					return;
				} else {
					repository.answeredIncorrect(currentQuestion);
					
					showingCorrectAnswer = true;
					radioGroup.setEnabled(false);
					
					final RadioButton correctButton = (RadioButton) findViewById(correctChoice);
					if (defaultBackground == null) {
						defaultBackground = correctButton.getBackground();
					}
					if (correctMarkBackground == 0) {
						correctMarkBackground = Color.rgb(0, 64, 0);
					}
					correctButton.setBackgroundColor(correctMarkBackground);
					
					return;
				}
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
		
		order = null;
		
		final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		radioGroup.clearCheck();
		
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
	
	private List<RadioButton> getRadioButtons() {
		final List<RadioButton> radioButtons = new LinkedList<RadioButton>();
		radioButtons.add((RadioButton) findViewById(R.id.radio0));
		radioButtons.add((RadioButton) findViewById(R.id.radio1));
		radioButtons.add((RadioButton) findViewById(R.id.radio2));
		radioButtons.add((RadioButton) findViewById(R.id.radio3));
		return radioButtons;
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
		
		final Question question = repository.getQuestion(currentQuestion);

		final TextView textView = (TextView) findViewById(R.id.textViewFrage);
        
		textView.setText(question.getQuestionText());
		
		final List<RadioButton> radioButtons = getRadioButtons();
		
		if (order == null) {
			order = new LinkedList<Integer>();

			for (int i = 0; i < 4; i++) {
				order.add(rand.nextInt(order.size() + 1), i);
			}
		}
		correctChoice = radioButtons.get(order.get(0)).getId();
		for (int i = 0; i < 4; i++) {
			radioButtons.get(order.get(i)).setText(question.getAnswers().get(i));
		}

		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
		progressBar.setProgress(question.getLevel());
		
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
	
    private ImageView getQuestionImage() {
        int imageResourceId = -1;
        switch (currentQuestion) {
        case 8961:
        	imageResourceId = R.drawable.q17;
        	break;
        case 8962:
        	imageResourceId = R.drawable.q18;
        	break;
        case 8963:
        	imageResourceId = R.drawable.q19;
        	break;
        case 8964:
        	imageResourceId = R.drawable.q20;
        	break;
        case 8965:
        	imageResourceId = R.drawable.q21;
        	break;
        case 8966:
        	imageResourceId = R.drawable.q22;
        	break;
        case 8967:
        	imageResourceId = R.drawable.q23;
        	break;
        case 8968:
        	imageResourceId = R.drawable.q24;
        	break;
        case 8969:
        	imageResourceId = R.drawable.q25;
        	break;
        case 8970:
        	imageResourceId = R.drawable.q26;
        	break;
        case 8971:
        	imageResourceId = R.drawable.q27;
        	break;
        case 8972:
        	imageResourceId = R.drawable.q28;
        	break;
        case 8973:
        	imageResourceId = R.drawable.q29;
        	break;
        case 8974:
        	imageResourceId = R.drawable.q30;
        	break;
        case 9052:
        	imageResourceId = R.drawable.q107;
        	break;
        case 9053:
        	imageResourceId = R.drawable.q108;
        	break;
        case 9055:
        	imageResourceId = R.drawable.q110;
        	break;
        case 9056:
        	imageResourceId = R.drawable.q111;
        	break;
        case 9057:
        	imageResourceId = R.drawable.q112;
        	break;
        case 9058:
        	imageResourceId = R.drawable.q113;
        	break;
        case 9059:
        	imageResourceId = R.drawable.q114;
        	break;
        case 9060:
        	imageResourceId = R.drawable.q115;
        	break;
        case 9061:
        	imageResourceId = R.drawable.q116;
        	break;
        case 9065:
        	imageResourceId = R.drawable.q120;
        	break;
        case 9066:
        	imageResourceId = R.drawable.q121;
        	break;
        case 9067:
        	imageResourceId = R.drawable.q122;
        	break;
        case 9068:
        	imageResourceId = R.drawable.q123;
        	break;
        case 9069:
        	imageResourceId = R.drawable.q124;
        	break;
        case 9070:
        	imageResourceId = R.drawable.q125;
        	break;
        case 9072:
        	imageResourceId = R.drawable.q127;
        	break;
        case 9074:
        	imageResourceId = R.drawable.q129;
        	break;
        case 9076:
        	imageResourceId = R.drawable.q131;
        	break;
        case 9077:
        	imageResourceId = R.drawable.q132;
        	break;
        case 9090:
        	imageResourceId = R.drawable.q145;
        	break;
        case 9091:
        	imageResourceId = R.drawable.q146;
        	break;
        case 9092:
        	imageResourceId = R.drawable.q147;
        	break;
        case 9093:
        	imageResourceId = R.drawable.q148;
        	break;
        case 9094:
        	imageResourceId = R.drawable.q149;
        	break;
        case 9095:
        	imageResourceId = R.drawable.q150;
        	break;
        case 9096:
        	imageResourceId = R.drawable.q151;
        	break;
        case 9097:
        	imageResourceId = R.drawable.q152;
        	break;
        case 9098:
        	imageResourceId = R.drawable.q153;
        	break;
        case 9099:
        	imageResourceId = R.drawable.q154;
        	break;
        case 9100:
        	imageResourceId = R.drawable.q155;
        	break;
        case 9101:
        	imageResourceId = R.drawable.q156;
        	break;
        case 9102:
        	imageResourceId = R.drawable.q157;
        	break;
        case 9125:
        	imageResourceId = R.drawable.q180;
        	break;
        case 9128:
        	imageResourceId = R.drawable.q183;
        	break;
        case 9130:
        	imageResourceId = R.drawable.q185;
        	break;
        case 9131:
        	imageResourceId = R.drawable.q186;
        	break;
        case 9133:
        	imageResourceId = R.drawable.q188;
        	break;
        case 9134:
        	imageResourceId = R.drawable.q189;
        	break;
        case 9137:
        	imageResourceId = R.drawable.q192;
        	break;
        case 9138:
        	imageResourceId = R.drawable.q193;
        	break;
        case 9141:
        	imageResourceId = R.drawable.q196;
        	break;
        case 9143:
        	imageResourceId = R.drawable.q198;
        	break;
        case 9144:
        	imageResourceId = R.drawable.q199;
        	break;
        case 9145:
        	imageResourceId = R.drawable.q200;
        	break;
        case 9146:
        	imageResourceId = R.drawable.q201;
        	break;
        case 9147:
        	imageResourceId = R.drawable.q202;
        	break;
        case 9148:
        	imageResourceId = R.drawable.q203;
        	break;
        case 9149:
        	imageResourceId = R.drawable.q204;
        	break;
        case 9188:
        	imageResourceId = R.drawable.q243;
        	break;
        case 9189:
        	imageResourceId = R.drawable.q244;
        	break;
        case 9241:
        	imageResourceId = R.drawable.q295;
        	break;
        default:
            return null;
        }

        final ImageView image = new ImageView(this);
        // image.setBackgroundColor(Color.WHITE);
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
}
