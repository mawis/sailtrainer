package eu.wimmerinformatik.lrc;

import eu.wimmerinformatik.lrc.data.Repository;
import eu.wimmerinformatik.lrc.R;
import android.app.Activity;
import android.content.Intent;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class LRCTrainerActivity extends Activity {
	private Repository repository;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final ListView topicList = (ListView) findViewById(R.id.listView1);
        
        repository = new Repository(this);
        
        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.topic_list_item, null, new String[]{"name"}, new int[]{R.id.topicListItem});
        topicList.setAdapter(adapter);
        repository.setTopicsInSimpleCursorAdapter(adapter);
        
        topicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			//@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				
				final Intent intent = new Intent(LRCTrainerActivity.this, QuestionAsker.class);
				intent.putExtra(QuestionAsker.class.getName() + ".topic", id);
				startActivity(intent);
			}
		});
    }
}
