package eu.wimmerinformatik.src;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import eu.wimmerinformatik.src.R;
import eu.wimmerinformatik.src.data.Repository;
import eu.wimmerinformatik.src.data.TopicStats;

public class StatisticsActivity extends Activity {
	private Repository repository;
	private int topicId;

	@Override
	public void onDestroy() {
		super.onDestroy();
		repository.close();
		repository = null;
	}
	
	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putLong(getClass().getName() + ".topic", topicId);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        repository = new Repository(this);
        
        if (savedInstanceState != null) {
        	topicId = (int) savedInstanceState.getLong(getClass().getName()+".topic");
        } else {
        	topicId = (int) getIntent().getExtras().getInt(getClass().getName()+".topic");
        }
        
        setContentView(R.layout.statistics);
        
        final TextView topicName = (TextView) findViewById(R.id.topicName);
        topicName.setText(repository.getTopic(topicId).getName());
        
        final TopicStats stats = repository.getTopicStat(topicId);
        
        final ProgressBar totalProgress = (ProgressBar) findViewById(R.id.totalProgress);
        totalProgress.setMax(stats.getMaxProgress());
        totalProgress.setProgress(stats.getCurrentProgress());
        
        final ProgressBar atLevel0 = (ProgressBar) findViewById(R.id.atLevel0);
        atLevel0.setMax(stats.getQuestionCount());
        atLevel0.setProgress(stats.getQuestionsAtLevel()[0]);
        
        final ProgressBar atLevel1 = (ProgressBar) findViewById(R.id.atLevel1);
        atLevel1.setMax(stats.getQuestionCount());
        atLevel1.setProgress(stats.getQuestionsAtLevel()[1]);

        final ProgressBar atLevel2 = (ProgressBar) findViewById(R.id.atLevel2);
        atLevel2.setMax(stats.getQuestionCount());
        atLevel2.setProgress(stats.getQuestionsAtLevel()[2]);

        final ProgressBar atLevel3 = (ProgressBar) findViewById(R.id.atLevel3);
        atLevel3.setMax(stats.getQuestionCount());
        atLevel3.setProgress(stats.getQuestionsAtLevel()[3]);
        
        final ProgressBar atLevel4 = (ProgressBar) findViewById(R.id.atLevel4);
        atLevel4.setMax(stats.getQuestionCount());
        atLevel4.setProgress(stats.getQuestionsAtLevel()[4]);

        final ProgressBar atLevel5 = (ProgressBar) findViewById(R.id.atLevel5);
        atLevel5.setMax(stats.getQuestionCount());
        atLevel5.setProgress(stats.getQuestionsAtLevel()[5]);
	}
}
