package luft27.androidbeginner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onOk(View view) {
        EditText name = (EditText) findViewById(R.id.name);
        TextView message = (TextView) findViewById(R.id.message);

        message.setText("Hello, " + name.getText());
    }
}
