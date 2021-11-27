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

    private TextView ipfsTitle;
    private ProgressBar ipfsProgress;
    private TextView ipfsResult;

    private TextView peerCounter;

    private Button xkcdButton;
    private TextView xkcdStatus;
    private ProgressBar xkcdProgress;
    private TextView xkcdError;

    private EditText messageContent;
    private Button sendMessage;
    private TextView messageInfo;

    private PeerCounter peerCounterUpdater;

    void setIpfs(IPFS ipfs) {
        this.ipfs = ipfs;
    }

    IPFS getIpfs() {
        return ipfs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipfsTitle = findViewById(R.id.ipfsTitle);
        ipfsProgress = findViewById(R.id.ipfsProgress);
        ipfsResult = findViewById(R.id.ipfsResult);

        peerCounter = findViewById(R.id.peerCounter);

        xkcdButton = findViewById(R.id.xkcdButton);
        xkcdStatus = findViewById(R.id.xkcdStatus);
        xkcdProgress = findViewById(R.id.xkcdProgress);
        xkcdError = findViewById(R.id.xkcdError);

        messageContent = findViewById(R.id.message_content);
        sendMessage = findViewById(R.id.send_message);
        messageInfo = findViewById(R.id.message_info);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM);
        }

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
//                    byte[] sData = ipfs.newRequest("cat")
//                        .withArgument("/ip4/1.193.164.188/tcp/57949/p2p/12D3KooWLDDUVywLezySdtaFB8zbfmY78uqXM3qTne2jPct7ykSB/QmXCYTn6fBXsiyArdY3CivZQkJFCjkNMYTrvL9FLafMgsv")
//                        .send();
//                    Log.d(TAG, "onClick: sData " + sData);

                    String res = ipfs.add(messageContent.getText().toString().getBytes());

                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("cid", res);
                    clipboard.setPrimaryClip(clipData);
                    Toast.makeText(MainActivity.this, "复制成功: " + res, Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "onClick: " + res);

                    byte[] data = ipfs.newRequest("cat")
                        .withArgument(res)
                        .send();
                    Log.d(TAG, "onClick: data " + new String(data));

                    messageInfo.setText("分享链接: https://ipfs.io/ipfs/" + res + "\n"
                        + "CID: " + res);

                    byte[] bootStrapData = ipfs.newRequest("bootstrap")
                        .withArgument("add")
                        .withArgument("/ip4/39.108.226.205/tcp/4001/p2p/12D3KooWR4g6cp5abx8PNUXFZPuajRLMvCPFmCGjvWS8ZhBCs1x3")
                        .send();

                    Log.d(TAG, "onClick: bootStrapData " + new String(bootStrapData));

                    byte[] catData = ipfs.newRequest("cat")
                        .withArgument("QmXuxgT89nEQniQKohFN1cJ695JkS2Ssho12ioCixAB2vq")
                        .send();

                    Log.d(TAG, "onClick: catData " + new String(catData));

//                    ArrayList<JSONObject> jsonList = ipfs.newRequest("id").sendToJSONList();
//
//                    Log.d(TAG, "doInBackground: " + jsonList.toString());
//                    JSONArray addresses = jsonList.get(0).getJSONArray("Addresses");
//                    for (int i = 0; i < addresses.length(); i++) {
//                        Log.d(TAG, "doInBackground: address " + addresses.get(i).toString());
//                    }
                } catch (RequestBuilder.RequestBuilderException e) {
                    e.printStackTrace();
                } catch (IPFS.ShellRequestException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        new StartIPFS(this).execute();

        final MainActivity activity = this;
        xkcdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FetchRandomXKCD(activity).execute();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (peerCounterUpdater != null) {
            peerCounterUpdater.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (peerCounterUpdater != null) {
            peerCounterUpdater.start();
        }
    }

    void displayPeerIDError(String error) {
        ipfsTitle.setTextColor(Color.RED);
        ipfsResult.setTextColor(Color.RED);

        ipfsTitle.setText(getString(R.string.titlePeerIDErr));
        ipfsResult.setText(error);
        ipfsProgress.setVisibility(View.INVISIBLE);
    }

    void displayPeerIDResult(final String peerID) {
        ipfsTitle.setText("点击复制 " + getString(R.string.titlePeerID));
        ipfsResult.setText(peerID);
        ipfsProgress.setVisibility(View.INVISIBLE);

        updatePeerCount(0);
        peerCounter.setVisibility(View.VISIBLE);
//        xkcdButton.setVisibility(View.VISIBLE);

        peerCounterUpdater = new PeerCounter(this, 1000);
        peerCounterUpdater.start();

        ipfsResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("peerID", peerID);
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(MainActivity.this, "复制成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    void updatePeerCount(int count) {
        peerCounter.setText(getString(R.string.titlePeerCon, count));
    }

    void displayFetchProgress() {
        xkcdStatus.setTextColor(ipfsTitle.getCurrentTextColor());
        xkcdStatus.setText(R.string.titleFetching);
        xkcdStatus.setVisibility(View.VISIBLE);
        xkcdError.setVisibility(View.INVISIBLE);
        xkcdProgress.setVisibility(View.VISIBLE);

        xkcdButton.setAlpha(0.5f);
        xkcdButton.setClickable(false);
    }

    void displayFetchSuccess() {
        xkcdStatus.setVisibility(View.INVISIBLE);
        xkcdProgress.setVisibility(View.INVISIBLE);

        xkcdButton.setAlpha(1);
        xkcdButton.setClickable(true);
    }

    void displayFetchError(String error) {
        xkcdStatus.setTextColor(Color.RED);
        xkcdStatus.setText(R.string.titleFetchingErr);

        xkcdProgress.setVisibility(View.INVISIBLE);
        xkcdError.setVisibility(View.VISIBLE);
        xkcdError.setText(error);

        xkcdButton.setAlpha(1);
        xkcdButton.setClickable(true);
    }

    static String exceptionToString(Exception error) {
        String string = error.getMessage();

        if (error.getCause() != null) {
            string += ": " + error.getCause().getMessage();
        }

        return string;
    }
}
