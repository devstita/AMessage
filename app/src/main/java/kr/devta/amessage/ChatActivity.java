package kr.devta.amessage;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity implements Runnable {
    public static ActivityStatus status = null;
    private Thread checkNetworkThread = null;

    Toolbar toolbar;
    ListView chatingListView;
    EditText messageEditText;
    Button sendButton;

    FriendInfo friendInfo;
    public static ChatingListViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Manager.showActivityName(this);

        status = ActivityStatus.CREATED;
        checkNetworkThread = new Thread(this);

        toolbar = findViewById(R.id.chat_Toolbar);
        chatingListView = findViewById(R.id.chat_ChatingListView);
        messageEditText = findViewById(R.id.chat_MessageEditText);
        sendButton = findViewById(R.id.chat_SendButton);

        friendInfo = (FriendInfo) getIntent().getSerializableExtra("FriendInfo");
        adapter = new ChatingListViewAdapter(getApplicationContext(), friendInfo);

        checkNetworkThread.start();

        toolbar.setTitle(friendInfo.getName());
        toolbar.setSubtitle(" - " + friendInfo.getPhone());

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        chatingListView.setAdapter(adapter);

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (messageEditText.getText().toString().contains("\n")) {
                    Manager.print("Enter key Clicked!!");
                    messageEditText.setText(messageEditText.getText().toString().replace("\n", ""));
                    sendButton.performClick();
                } else {
                    if (s.toString().isEmpty()) sendButton.setEnabled(false);
                    else sendButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sendButton.setEnabled(false);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                if (message == null || message.isEmpty()) return;
                messageEditText.setText("");

                ChatInfo chatInfo = new ChatInfo(message);

                Manager.send(friendInfo, chatInfo);

                adapter.addItem(chatInfo).refresh();
                Manager.addChat(1, friendInfo, chatInfo, true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = ActivityStatus.RESUMED;
        if (checkNetworkThread == null) checkNetworkThread = new Thread(this);
        Manager.chatAcitivtyCheckNetworkThreadFlag = true;
        if (checkNetworkThread.getState() == Thread.State.NEW) checkNetworkThread.start();

        adapter.clear();
        ArrayList<ChatInfo> chats = Manager.readChat(friendInfo);
        for (ChatInfo chat : chats) {
            adapter.addItem(chat);
        }
        adapter.refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();

        status = ActivityStatus.PAUSED;
        Manager.chatAcitivtyCheckNetworkThreadFlag = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        status = ActivityStatus.DESTROYED;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.chatMenu_Information:
                Intent intent = new Intent(getApplicationContext(), ChatSettingActivity.class);
                intent.putExtra("FriendInfo", friendInfo);
                startActivityForResult(intent, Manager.REQUEST_CODE_CHAT_SETTING);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Manager.REQUEST_CODE_CHAT_SETTING && resultCode == RESULT_OK) {
            switch (data.getStringExtra("Action")) {
                case "Remove":
                    finish();
                    break;
                case "ChangeName":
                    friendInfo = (Manager.getUpdatedFriendInfo(friendInfo));
                    toolbar.setTitle(friendInfo.getName());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void run() {
        while (Manager.chatAcitivtyCheckNetworkThreadFlag) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Manager.checkFriendNetwork(friendInfo, new Manager.ToDoAfterCheckNetworking() {
                @Override
                public void run(final boolean status) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageEditText.setHint((status) ? "aMessage 로 전송" : "SMS 로 전송");
                        }
                    });
                }
            });
        }
        Manager.chatAcitivtyCheckNetworkThreadFlag = true;
    }
}
