/**
 * 
 */
package eu.wimmerinformatik.trainer;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * @author matthias
 *
 */
public class QuestionAsker extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.question_asker);
        
        final TextView textView = (TextView) findViewById(R.id.textViewFrage);
        
        if (savedInstanceState != null) {
        	textView.setText("Topic (saved): " + savedInstanceState.getInt(getClass().getName()+".topic"));
        } else {
        	textView.setText("Topic: " + getIntent().getExtras().getLong(QuestionAsker.class.getName()+".topic"));
        }
	}

}
