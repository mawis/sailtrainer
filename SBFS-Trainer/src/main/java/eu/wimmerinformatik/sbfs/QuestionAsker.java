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

package eu.wimmerinformatik.sbfs;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import eu.wimmerinformatik.sbfs.data.Question;
import eu.wimmerinformatik.sbfs.data.QuestionSelection;
import eu.wimmerinformatik.sbfs.data.Repository;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
	private int maxProgress;
	private int currentProgress;
	private Random rand = new Random();
	private List<Integer> order;
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
		rand = null;
		order = null;
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
			maxProgress = savedInstanceState.getInt(getClass().getName() + ".maxProgress");
			currentProgress = savedInstanceState.getInt(getClass().getName() + ".currentProgress");
        	
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

    /**
     * Populate the options menu.
     * 
     * @param menu the menu to populate
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.askermenu, menu);
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
				uri.append("http://sportboot.mobi/trainer/segeln/sbfs/app/help?question=");
				uri.append(currentQuestion);
				uri.append("&topic=");
				uri.append(topicId);
				uri.append("&view=QuestionAsker");
				final Intent intent2 = new Intent(Intent.ACTION_VIEW);
				intent2.setData(Uri.parse(uri.toString()));
				startActivity(intent2);
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

    private void restartTopic() {
		repository.resetTopic(topicId);
		nextQuestion();
    }

	private void showStandardView() {
        setContentView(R.layout.question_asker);
        showingStandardView = true;

		ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar1);
		progress.setMax(5);

		final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		final Button contButton = (Button) findViewById(R.id.button1);

		// only enable continue when answer is selected
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				contButton.setEnabled(radioGroup.getCheckedRadioButtonId() != -1);
			}

		});
	
        contButton.setOnClickListener(new View.OnClickListener() {

			// @Override
			public void onClick(View v) {

				// find what has been selected
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
				} else if (selectedButton != -1) {
					repository.answeredIncorrect(currentQuestion);
					
					showingCorrectAnswer = true;
					radioGroup.setEnabled(false);
					
					final RadioButton correctButton = (RadioButton) findViewById(correctChoice);
					correctButton.setBackgroundResource(R.color.correctAnswer);
					correctButton.setTextAppearance(QuestionAsker.this, R.style.correctAnswerStyle);
					
					return;
				} else {
					Toast.makeText(QuestionAsker.this, getString(R.string.noAnswerSelected), Toast.LENGTH_SHORT).show();
				}
			}
        });		
	}
	
	private void nextQuestion() {
		if (!showingStandardView) {
			showStandardView();
		}
		
		if (correctChoice != 0) {
			final RadioButton correctButton = (RadioButton) findViewById(correctChoice);
			correctButton.setBackgroundResource(0);
			correctButton.setTextAppearance(this, android.R.style.TextAppearance);
	}
		
		order = null;
		
		final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		radioGroup.clearCheck();
		final Button contButton = (Button) findViewById(R.id.button1);
		contButton.setEnabled(false);
		
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

		final TextView levelText = (TextView) findViewById(R.id.levelText);
		levelText.setText(question.getLevel() == 0 ? getString(R.string.firstPass) :
				question.getLevel() == 1 ? getString(R.string.secondPass) :
				question.getLevel() == 2 ? getString(R.string.thirdPass) :
				question.getLevel() == 3 ? getString(R.string.fourthPass) :
				question.getLevel() == 4 ? getString(R.string.fifthPass) :
				String.format(getString(R.string.passText), question.getLevel()));

		final TextView textView = (TextView) findViewById(R.id.textViewFrage);
        
		textView.setText(safeText(question.getQuestionText()));
		
		final List<RadioButton> radioButtons = getRadioButtons();
		
		if (order == null) {
			order = new LinkedList<Integer>();

			for (int i = 0; i < 4; i++) {
				order.add(rand.nextInt(order.size() + 1), i);
			}
		}
		correctChoice = radioButtons.get(order.get(0)).getId();
		for (int i = 0; i < 4; i++) {
			radioButtons.get(order.get(i)).setText(safeText(question.getAnswers().get(i)));
		}

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
	
    private ImageView getQuestionImage() {
        int imageResourceId = -1;
        switch (currentQuestion) {
        case 9605:
        	imageResourceId = R.drawable.q17;
        	break;
        case 9606:
        	imageResourceId = R.drawable.q18;
        	break;
        case 9607:
        	imageResourceId = R.drawable.q19;
        	break;
        case 9608:
        	imageResourceId = R.drawable.q20;
        	break;
        case 9609:
        	imageResourceId = R.drawable.q21;
        	break;
        case 9610:
        	imageResourceId = R.drawable.q22;
        	break;
        case 9611:
        	imageResourceId = R.drawable.q23;
        	break;
        case 9612:
        	imageResourceId = R.drawable.q24;
        	break;
        case 9613:
        	imageResourceId = R.drawable.q25;
        	break;
        case 9614:
        	imageResourceId = R.drawable.q26;
        	break;
        case 9615:
        	imageResourceId = R.drawable.q27;
        	break;
        case 9616:
        	imageResourceId = R.drawable.q28;
        	break;
        case 9617:
        	imageResourceId = R.drawable.q29;
        	break;
        case 9618:
        	imageResourceId = R.drawable.q30;
        	break;
        case 9680:
        	imageResourceId = R.drawable.q91;
        	break;
        case 9681:
        	imageResourceId = R.drawable.q92;
        	break;
        case 9682:
        	imageResourceId = R.drawable.q93;
        	break;
        case 9683:
        	imageResourceId = R.drawable.q94;
        	break;
        case 9686:
        	imageResourceId = R.drawable.q97;
        	break;
        case 9687:
        	imageResourceId = R.drawable.q98;
        	break;
        case 9688:
        	imageResourceId = R.drawable.q99;
        	break;
        case 9691:
        	imageResourceId = R.drawable.q102;
        	break;
        case 9692:
        	imageResourceId = R.drawable.q103;
        	break;
        case 9693:
        	imageResourceId = R.drawable.q104;
        	break;
        case 9694:
        	imageResourceId = R.drawable.q105;
        	break;
        case 9695:
        	imageResourceId = R.drawable.q106;
        	break;
        case 9696:
        	imageResourceId = R.drawable.q107;
        	break;
        case 9697:
        	imageResourceId = R.drawable.q108;
        	break;
        case 9698:
        	imageResourceId = R.drawable.q109;
        	break;
        case 9699:
        	imageResourceId = R.drawable.q110;
        	break;
        case 9700:
        	imageResourceId = R.drawable.q111;
        	break;
        case 9701:
        	imageResourceId = R.drawable.q112;
        	break;
        case 9704:
        	imageResourceId = R.drawable.q115;
        	break;
        case 9711:
        	imageResourceId = R.drawable.q122;
        	break;
        case 9712:
        	imageResourceId = R.drawable.q123;
        	break;
        case 9722:
        	imageResourceId = R.drawable.q133;
        	break;
        case 9723:
        	imageResourceId = R.drawable.q134;
        	break;
        case 9737:
        	imageResourceId = R.drawable.q148;
        	break;
        case 9738:
        	imageResourceId = R.drawable.q149;
        	break;
        case 9739:
        	imageResourceId = R.drawable.q150;
        	break;
        case 9740:
        	imageResourceId = R.drawable.q151;
        	break;
        case 9743:
        	imageResourceId = R.drawable.q154;
        	break;
        case 9765:
        	imageResourceId = R.drawable.q176;
        	break;
        case 9766:
        	imageResourceId = R.drawable.q177;
        	break;
        case 9768:
        	imageResourceId = R.drawable.q179;
        	break;
        case 9769:
        	imageResourceId = R.drawable.q180;
        	break;
        case 9771:
        	imageResourceId = R.drawable.q182;
        	break;
        case 9773:
        	imageResourceId = R.drawable.q184;
        	break;
        case 9774:
        	imageResourceId = R.drawable.q185;
        	break;
        case 9776:
        	imageResourceId = R.drawable.q187;
        	break;
        case 9778:
        	imageResourceId = R.drawable.q189;
        	break;
        case 9779:
        	imageResourceId = R.drawable.q190;
        	break;
        case 9780:
        	imageResourceId = R.drawable.q191;
        	break;
        case 9781:
        	imageResourceId = R.drawable.q192;
        	break;
        case 9782:
        	imageResourceId = R.drawable.q193;
        	break;
        case 9783:
        	imageResourceId = R.drawable.q194;
        	break;
        case 9784:
        	imageResourceId = R.drawable.q195;
        	break;
        case 9785:
        	imageResourceId = R.drawable.q196;
        	break;
        case 9789:
        	imageResourceId = R.drawable.q200;
        	break;
        case 9790:
        	imageResourceId = R.drawable.q201;
        	break;
        case 9791:
        	imageResourceId = R.drawable.q202;
        	break;
        case 9792:
        	imageResourceId = R.drawable.q203;
        	break;
        case 9793:
        	imageResourceId = R.drawable.q204;
        	break;
        case 9794:
        	imageResourceId = R.drawable.q205;
        	break;
        case 9795:
        	imageResourceId = R.drawable.q206;
        	break;
        case 9796:
        	imageResourceId = R.drawable.q207;
        	break;
        case 9797:
        	imageResourceId = R.drawable.q208;
        	break;
        case 9849:
        	imageResourceId = R.drawable.q260;
        	break;
        case 9853:
        	imageResourceId = R.drawable.q265;
        	break;
        case 9854:
        	imageResourceId = R.drawable.q266;
        	break;
        case 9872:
        	imageResourceId = R.drawable.q284;
        	break;
        case 9873:
        	imageResourceId = R.drawable.q285;
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

	private String safeText(final String source) {
		return replaceNNBSP && source != null ? source.replace('\u202f', '\u00a0') : source;
	}
}
