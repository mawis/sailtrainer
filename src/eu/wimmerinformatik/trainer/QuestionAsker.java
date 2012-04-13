/**
 * 
 */
package eu.wimmerinformatik.trainer;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import eu.wimmerinformatik.trainer.data.Question;
import eu.wimmerinformatik.trainer.data.QuestionSelection;
import eu.wimmerinformatik.trainer.data.Repository;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
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
		
		outState.putInt(getClass().getName() + ".currentQuestion", currentQuestion);
		outState.putLong(getClass().getName() + ".topic", topicId);
		outState.putLong(getClass().getName() + ".nextTime", nextTime == null ? null : nextTime.getTime());
		
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

		
		final Button contButton = (Button) findViewById(R.id.button1);
        contButton.setOnClickListener(new View.OnClickListener() {

			@Override
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
					Toast.makeText(QuestionAsker.this, "Richtig!", Toast.LENGTH_SHORT).show();
					
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
		
		setContentView(R.layout.no_more_questions_finished);
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
			setContentView(R.layout.no_more_questions_wait);
			
			final TextView nextTimeText = (TextView) findViewById(R.id.nextTimeText);
			if (nextTime.getTime() - new Date().getTime() < 86400000L) {
				nextTimeText.setText(DateFormat.getTimeInstance().format(nextTime));
			} else {
				nextTimeText.setText(DateFormat.getDateTimeInstance().format(nextTime));
			}
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
	}
}
