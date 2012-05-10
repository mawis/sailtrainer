package eu.wimmerinformatik.lrc;

import java.text.DateFormat;
import java.util.Date;

import eu.wimmerinformatik.lrc.data.Repository;
import eu.wimmerinformatik.lrc.R;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class LRCTrainerActivity extends Activity {
	private Repository repository;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
          
        repository = new Repository(this);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	final ListView topicList = (ListView) findViewById(R.id.listView1);
        
        final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.topic_list_item, null, new String[]{"name", "status", "next_question"}, new int[]{R.id.topicListItem, R.id.topicStatusView, R.id.nextQuestionTime});
        adapter.setViewBinder(new ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				
				if (columnIndex == 4) {
					final TextView textView = (TextView) view;
					if (!cursor.isNull(4)) {
						final long nextQuestion = cursor.getLong(4);
						final long now = new Date().getTime();
						if (nextQuestion > now) {
							
							if (nextQuestion - now < 64800000L) {
								textView.setText(getString(R.string.nextLabel) + DateFormat.getTimeInstance().format(new Date(nextQuestion)));
							} else {
								textView.setText(getString(R.string.nextLabel) + DateFormat.getDateTimeInstance().format(new Date(nextQuestion)));
							}
							return true;
						}
					}
					textView.setText("");
					return true;
				}
				
				return false;
			}
        });
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
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	final ListView topicList = (ListView) findViewById(R.id.listView1);
    	
    	final SimpleCursorAdapter adapter = (SimpleCursorAdapter) topicList.getAdapter();
    	final Cursor previousCursor = adapter.getCursor();
    	adapter.changeCursor(null);
    	previousCursor.close();
    	topicList.setAdapter(null);
    }
}
