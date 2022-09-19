package com.yusril.chatbotcovid_19;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.Graphmaster;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ChatBot extends AppCompatActivity {
    private static final int STORAGE_CODE = 1000;
    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private ImageView buttonSend, pdf, delete,back;
    private boolean side = true;
    public Bot bot;
    public static Chat chat;
    public int pos;
    ArrayList<String> arrayList = new ArrayList<>();

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat_bot);
        buttonSend = (ImageView) findViewById(R.id.send);
        listView = (ListView) findViewById(R.id.msgview);
        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.right);
        chatText = (EditText) findViewById(R.id.msg);

        back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        pdf = findViewById(R.id.pdf);
        pdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_DENIED) {
                        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permissions, STORAGE_CODE);
                    } else {
                        savePdf();
                    }
                } else {
                    savePdf();
                }
            }
        });
        delete = findViewById(R.id.hapus);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager clearmanager = getFragmentManager();
                ClearDialogFragment clearDialogFragment = new ClearDialogFragment();
                clearDialogFragment.show(clearmanager, "theDialog");
            }
        });

        if (savedInstanceState != null) {
            ArrayList<ChatMessage> values = savedInstanceState.getParcelableArrayList("key");
            if (values != null) {
                chatArrayAdapter.addAll(values);
                chatArrayAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Retrieved data", Toast.LENGTH_LONG).show();
            }
        }
        listView.setAdapter(chatArrayAdapter);

        AssetManager assets = getResources().getAssets();
        File jayDir = new File(getCacheDir().toString() + "/chatbot/bots/bot");
        boolean b = jayDir.mkdirs();

        if (jayDir.exists()) {
            try {
                for (String dir : assets.list("bot")) {
                    File subdir = new File(jayDir.getPath() + "/" + dir);
                    boolean subdir_check = subdir.mkdirs();
                    for (String file : assets.list("bot/" + dir)) {
                        File f = new File(jayDir.getPath() + "/" + dir + "/" + file);
                        if (f.exists()) {
                            continue;
                        }
                        InputStream in = null;
                        OutputStream out = null;
                        in = assets.open("bot/" + dir + "/" + file);
                        out = new FileOutputStream(jayDir.getPath() + "/" + dir + "/" + file);
                        copyFile(in, out);
                        in.close();
                        out.flush();
                        out.close();

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MagicStrings.root_path = getCacheDir().toString() + "/chatbot";
        System.out.println("Working Directory = " + MagicStrings.root_path);
        AIMLProcessor.extension = new PCAIMLProcessorExtension();
        bot = new Bot("bot", MagicStrings.root_path, "chat");
        chat = new Chat(bot);

        String welcome = "welcome";
        String temp = mainFunction(welcome);
        arrayList.add("Bot : "+temp+"\n\n");
        side = !side;
        chatArrayAdapter.add(new ChatMessage(side, temp));
        side = !side;
        Log.v("LISTCHAT","INI DIA: "+arrayList);

    }

    @Override
    public void onStart() {
        super.onStart();
//        arrayList.clear();
        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    return sendChatMessage();
                }
                return false;
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                sendChatMessage();
            }
        });

        listView.setAdapter(chatArrayAdapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager delmanager = getFragmentManager();
                DeleteMessage delDialogFragment = new DeleteMessage();
                delDialogFragment.show(delmanager, "theDialog");
                pos = position;
                return false;
            }
        });
    }

    public void onStop(Bundle saveinstance) {
        super.onStop();
        super.onSaveInstanceState(saveinstance);
        ArrayList<ChatMessage> messages = chatArrayAdapter.getList();
        saveinstance.putParcelableArrayList("key", messages);
    }

    public void delMessageBool() {
        chatArrayAdapter.removeThis(pos);
        arrayList.remove(pos);
        chatArrayAdapter.notifyDataSetChanged();
        listView.setAdapter(chatArrayAdapter);
    }


    //copying the file
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static String mainFunction(String args) {
        MagicBooleans.trace_mode = false;
        System.out.println("trace mode = " + MagicBooleans.trace_mode);
        Graphmaster.enableShortCuts = true;
        String request = args;
        String response = chat.multisentenceRespond(request);
        return response;
    }

    public boolean sendChatMessage() {
        String comp = chatText.getText().toString();
        if (TextUtils.isEmpty(comp)) {
            Snackbar.make(findViewById(android.R.id.content), "Pesan belum di isi!!!", Snackbar.LENGTH_LONG)
                    .show();
            chatText.setError("Pesan kosong");
        } else {
            chatArrayAdapter.add(new ChatMessage(side, chatText.getText().toString()));
            arrayList.add("User : "+chatText.getText().toString());
            chatText.setText("");
            String temp = mainFunction(comp);
            arrayList.add("Bot : "+temp+"\n\n");
            side = !side;
            chatArrayAdapter.add(new ChatMessage(side, temp));
            side = !side;
            Log.v("LISTCHAT","INI DIA: "+arrayList);
            return true;
        }
        return false;
    }

    public void clearchat() {
        chatArrayAdapter.clearData();
        arrayList.clear();
        chatArrayAdapter.notifyDataSetChanged();
        listView.setAdapter(chatArrayAdapter);
    }

    private void savePdf() {
        Document mDoc = new Document();
        String mFileName = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(System.currentTimeMillis());
        String mFilePath = Environment.getExternalStorageDirectory() + "/" + mFileName + ".pdf";

        try {
            PdfWriter.getInstance(mDoc, new FileOutputStream(mFilePath));
            mDoc.open();
            String mText = String.valueOf(arrayList);
            mDoc.addAuthor("ChatBot Covid");
            for (int i=0; i < arrayList.size(); i++)
            {
                String ini = arrayList.get(i);
                mDoc.add(new Paragraph(ini +"\n"));
                Log.v("LISTCHAT","nilai: "+ini);
            }
            mDoc.close();
            Toast.makeText(getApplicationContext(), mFileName + ".pdf\nTersimpan" + mFilePath, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case STORAGE_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    savePdf();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}