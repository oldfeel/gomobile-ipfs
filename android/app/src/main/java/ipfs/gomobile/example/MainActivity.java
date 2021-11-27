package ipfs.gomobile.example;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import ipfs.gomobile.android.IPFS;
import ipfs.gomobile.android.RequestBuilder;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "oldfeel";
    private static final int REQUEST_PERM = 1;
    private IPFS ipfs;

    private TextView ipfsStatus;
    private EditText content;
    private Button sendFile;
    private ListView listView;
    private EditText cid;
    private Button getContent;
    private TextView info;

    void setIpfs(IPFS ipfs) {
        this.ipfs = ipfs;

        try {
            byte[] bootStrapData = ipfs.newRequest("bootstrap")
                .withArgument("add")
                .withArgument("/ip4/39.108.226.205/tcp/4001/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3")
                .send();

            Log.d(TAG, "onClick: bootStrapData " + new String(bootStrapData));

            ipfs.pubSubSubscribe("oldfeel", new IPFS.SubListener() {
                @Override
                public void onListener(String var1) {
                    Log.d(TAG, "onListener: " + var1);
                }
            });
        } catch (RequestBuilder.RequestBuilderException e) {
            e.printStackTrace();
        } catch (IPFS.ShellRequestException e) {
            e.printStackTrace();
        }
    }

    IPFS getIpfs() {
        return ipfs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipfsStatus = findViewById(R.id.ipfs_status);
        content = findViewById(R.id.content);
        sendFile = findViewById(R.id.send_file);
        listView = findViewById(R.id.list_view);
        cid = findViewById(R.id.cid);
        getContent = findViewById(R.id.get_content);
        info = findViewById(R.id.info);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM);
//        }

        sendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String res = ipfs.add(content.getText().toString().getBytes());
                    info.setText("上传成功, CID: " + res);

                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("cid", res);
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(MainActivity.this, "CID复制成功: " + res, Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        getContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    byte[] data = ipfs.newRequest("cat")
                        .withArgument(cid.getText().toString())
                        .send();

                    info.setText("获取内容成功: " + new String(data));
                } catch (RequestBuilder.RequestBuilderException e) {
                    e.printStackTrace();
                } catch (IPFS.ShellRequestException e) {
                    e.printStackTrace();
                }

            }
        });

        new StartIPFS(this).execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    void displayPeerIDResult(final String peerID) {
        ipfsStatus.setText("连接成功,本机 peerID: " + peerID);

        ipfsStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("peerID", peerID);
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(MainActivity.this, "复制成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    static String exceptionToString(Exception error) {
        String string = error.getMessage();

        if (error.getCause() != null) {
            string += ": " + error.getCause().getMessage();
        }

        return string;
    }
}
