package roboniania.com.roboniania_android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import roboniania.com.roboniania_android.PairingRobot;
import roboniania.com.roboniania_android.R;
import roboniania.com.roboniania_android.api.RoboService;
import roboniania.com.roboniania_android.api.model.NewGame;
import roboniania.com.roboniania_android.api.model.NewJob;
import roboniania.com.roboniania_android.api.model.NewRobot;
import roboniania.com.roboniania_android.api.model.NewStatus;
import roboniania.com.roboniania_android.api.model.NewTransaction;
import roboniania.com.roboniania_android.api.model.Transaction;
import roboniania.com.roboniania_android.storage.SharedPreferenceStorage;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String GAME_EXTRA_KEY = "game";
    private Toolbar toolbar;
    private SharedPreferenceStorage userLocalStorage;
    private Button start, play, stop;
    private static final String TAG = GameActivity.class.getSimpleName();
    private Context context;
    private NewGame game;
    private String uuid = null;
    private List<NewRobot> robotsList;
    private ProgressDialog progress;
    private String robotId, gameId, robotIp, transactionId = null;
    private static final int port = 3456;
    private int counter = 0;
    private boolean isStarting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        initComponents();
    }

    private void initComponents() {
        userLocalStorage = new SharedPreferenceStorage(this);
        context = getApplicationContext();

        start = (Button) findViewById(R.id.start);
        start.setOnClickListener(this);

        play = (Button) findViewById(R.id.play);
        play.setOnClickListener(this);

        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(this);

        //SETTING UP TOOLBAR
        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent i = getIntent();
        game = (NewGame) i.getExtras().getSerializable(GAME_EXTRA_KEY);
        this.gameId = game.getId();
        showGame(game);
    }

    public void downloadRobotList() {

        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RoboService.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        RoboService roboService = retrofit.create(RoboService.class);

        Call<List<NewRobot>> call = roboService.getRobotsList(userLocalStorage.getAccessToken());

        call.enqueue(new Callback<List<NewRobot>>() {
            @Override
            public void onResponse(Call<List<NewRobot>> call, Response<List<NewRobot>> response) {
                int statusCode = response.code();
                if (response.isSuccessful()) {
                    robotsList = response.body();
                    showRobotsPopupList();

                    Log.d(TAG, Integer.toString(statusCode));

                } else {
                    Log.d(TAG, Integer.toString(statusCode));
                }
            }

            @Override
            public void onFailure(Call<List<NewRobot>> call, Throwable t) {
                Toast.makeText(context, R.string.check_connection, Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void showGame(NewGame game) {
        TextView title = (TextView) findViewById(R.id.gameDetailTitle);
        ImageView icon = (ImageView) findViewById(R.id.gameDetailIcon);
        TextView description = (TextView) findViewById(R.id.gameDescription);
        ImageView gamePic = (ImageView) findViewById(R.id.game_pic);
        TextView version = (TextView) findViewById(R.id.version);
        TextView model = (TextView) findViewById(R.id.neededModel);
        TextView system = (TextView) findViewById(R.id.system);
        TextView author = (TextView) findViewById(R.id.author);

        title.setText(game.getName());
        icon.setImageResource(game.getIconId());
        description.setText(game.getDescription());
        gamePic.setImageResource(game.getPhotoId());

        version.setText(game.getVersion());
        model.setText(game.getRobot_model());
        system.setText(game.getRobot_system());
        author.setText(game.getAuthor());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(transactionId != null) {
                    showCancelTransactionDialog();
                } else {
                    NavUtils.navigateUpFromSameTask(this);
                }
                return true;
            case R.id.add:
                PairingRobot.showPairDialog(this, userLocalStorage);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCancelTransactionDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Are you sure you want to cancel transaction?");
        alert.setTitle("Cancel transaction..");

        alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cancelTransaction();
                    }
                }).start();
            }
        });

        alert.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.create();
        alert.show();
    }

    private void cancelTransaction() {
        try {
            Socket client = new Socket();
            client.connect(new InetSocketAddress(robotIp, port), 10000);

            DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());

            String rawJSON = "{\n" +
                    "\"transaction_id\": \"" + transactionId + "\",\n" +
                    "\"job\": \"ABORTED\"\n" +
                    "}";

            outToServer.writeUTF(rawJSON);

            DataInputStream in = new DataInputStream(client.getInputStream());

            int responseCode = in.readInt();

            client.close();
            System.out.println(responseCode);

            NavUtils.navigateUpFromSameTask(this);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                downloadRobotList();
                break;
            case R.id.play:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startPlaying();
                    }
                }).start();
                break;
            case R.id.stop:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stopPlaying();
                    }
                }).start();
                break;

        }

    }

    private void stopPlaying() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showStopDialog();
                }
            });

            Socket client = new Socket();
            client.connect(new InetSocketAddress(robotIp, port), 10000);

            DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());

            String rawJSON = "{\n" +
                    "\"transaction_id\": \"" + transactionId + "\",\n" +
                    "\"job\": \"ABORTED\"\n" +
                    "}";

            outToServer.writeUTF(rawJSON);

            DataInputStream in = new DataInputStream(client.getInputStream());

            int responseCode = in.readInt();
            switch(responseCode) {
                case 200:
                    startPolling();
                    break;
                case 404:
                case 400:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.cannot_connect, Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
            }
            client.close();
            System.out.println(responseCode);
        } catch(Exception e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, R.string.cannot_connect, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showStopDialog() {
        progress = new ProgressDialog(GameActivity.this, R.style.AppTheme_Dialog);
        progress.setTitle(R.string.stopping);
        progress.setMessage("Stopping game");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setProgressNumberFormat(null);
        progress.setProgressPercentFormat(null);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();
    }

    private void showRobotsPopupList() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(GameActivity.this);
        builderSingle.setIcon(R.drawable.robot);
        builderSingle.setTitle(R.string.choose_robot);
        builderSingle.setCancelable(false);

        final ArrayAdapter<NewRobot> arrayAdapter = new ArrayAdapter<>(GameActivity.this, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.addAll(robotsList);

        builderSingle.setNegativeButton(
                R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        if(arrayAdapter.isEmpty()) {
            builderSingle.setMessage("You need to pair any robot with your account first.");
        }

        builderSingle.setAdapter(
                arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        robotIp = arrayAdapter.getItem(which).getRobot_ip();
                        robotId = arrayAdapter.getItem(which).getRobot_id();
                        sendTransactionRequest(robotId, gameId);
                        launchProgressDialog();
                    }
                });
        builderSingle.show();
    }

    private void sendTransactionRequest(String robotId, String gameId) {
        NewTransaction newTransaction = new NewTransaction(robotId, gameId);

        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(RoboService.ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

        RoboService roboService = retrofit.create(RoboService.class);

        Call<Transaction> call = roboService.createGameTransaction(userLocalStorage.getAccessToken(), newTransaction);

        call.enqueue(new Callback<Transaction>() {
            @Override
            public void onResponse(Call<Transaction> call, Response<Transaction> response) {
                int statusCode = response.code();
                if (response.isSuccessful()) {
                    final Transaction transaction = response.body();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendTransactionToRobot(transaction, robotIp);
                        }
                    }).start();

                    Log.d(TAG, Integer.toString(statusCode));
                } else {
                    progress.dismiss();
                    Log.d(TAG, Integer.toString(statusCode));
                    Toast.makeText(context, R.string.robot_is_busy, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Transaction> call, Throwable t) {
                Toast.makeText(context, R.string.check_connection, Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void sendTransactionToRobot(Transaction transaction, String robotIp) {
        transactionId = transaction.getTransaction_id();
        try {
            Socket client = new Socket();
            client.connect(new InetSocketAddress(robotIp, port), 10000);
            DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());

            String rawJSON = "{\n" +
                    "\"transaction_id\": \"" + transaction.getTransaction_id() + "\",\n" +
                    "\"status\": \"" + transaction.getStatus() + "\",\n" +
                    "\"account_id\": \"" + transaction.getAccount_id() + "\",\n" +
                    "\"game_id\": \"" + transaction.getGame_id() + "\",\n" +
                    "\"created_at\": \"" + transaction.getCreated_at()+ "\"\n" +
                    "}";

            outToServer.writeUTF(rawJSON);

            DataInputStream in = new DataInputStream(client.getInputStream());
            int responseCode = in.readInt();
            switch(responseCode) {
                case 200:
                    startPolling();
                    break;
                case 404:
                case 400:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            Toast.makeText(context, R.string.cannot_connect, Toast.LENGTH_SHORT).show();
                        }
                    });

                    break;
            }
            client.close();
            System.out.println(responseCode);
        } catch(Exception e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress.dismiss();
                    Toast.makeText(context, R.string.cannot_connect, Toast.LENGTH_SHORT).show();
                }
            });
            changeTransactionStatus(robotId, transactionId, new NewStatus("COMPLETED"));
            transactionId = null;
        }
    }

    private void changeTransactionStatus(String robotId, final String transactionId, NewStatus status) {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RoboService.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        RoboService roboService = retrofit.create(RoboService.class);

        Call<Void> call = roboService.changeTransactionStatus(robotId, transactionId, status);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int statusCode = response.code();
                if (response.isSuccessful()) {
                    Log.d(TAG, Integer.toString(statusCode));
                } else {
                    Log.d(TAG, Integer.toString(statusCode));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }

        });
    }

    private void startPolling() throws InterruptedException {
        this.counter = 0;
        while(this.counter < 10) {
            Gson gson = new GsonBuilder().create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(RoboService.ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            RoboService roboService = retrofit.create(RoboService.class);

            Call<Transaction> call = roboService.checkTransactionStatus(userLocalStorage.getAccessToken(), transactionId);

            call.enqueue(new Callback<Transaction>() {
                @Override
                public void onResponse(Call<Transaction> call, Response<Transaction> response) {
                    int statusCode = response.code();
                    if (response.isSuccessful()) {
                        Transaction transaction = response.body();

                        handleStatus(transaction);

                        Log.d(TAG, Integer.toString(statusCode));
                    } else {
                        Log.d(TAG, Integer.toString(statusCode));
                    }
                }

                @Override
                public void onFailure(Call<Transaction> call, Throwable t) {
                    Toast.makeText(context, R.string.check_connection, Toast.LENGTH_SHORT).show();
                }

            });
            Thread.sleep(2000);
        }

    }

    private void handleStatus(Transaction transaction) {
        String status = transaction.getStatus();
        System.out.println(status);
        switch(status) {
            case "DOWNLOADING":
                progress.setTitle(R.string.download);
                progress.setMessage("Downloading game..");
                start.setVisibility(View.VISIBLE);
                start.setEnabled(true);
                play.setVisibility(View.GONE);
                play.setEnabled(false);
                stop.setVisibility(View.GONE);
                stop.setEnabled(false);
                this.counter++;
                break;
            case "NEW":
                progress.setTitle(R.string.waiting);
                progress.setMessage("Waiting for robot..");
                start.setVisibility(View.VISIBLE);
                start.setEnabled(true);
                play.setVisibility(View.GONE);
                play.setEnabled(false);
                stop.setVisibility(View.GONE);
                stop.setEnabled(false);
                this.counter++;
                break;
            case "READY":
                if(isStarting) {
                    start.setVisibility(View.GONE);
                    start.setEnabled(false);
                    play.setVisibility(View.GONE);
                    play.setEnabled(false);
                    stop.setVisibility(View.VISIBLE);
                    stop.setEnabled(true);
                    isStarting = false;
                    break;
                }
                Toast.makeText(context, R.string.downloaded_game, Toast.LENGTH_SHORT).show();
                start.setVisibility(View.GONE);
                start.setEnabled(false);
                play.setVisibility(View.VISIBLE);
                play.setEnabled(true);
                stop.setVisibility(View.GONE);
                stop.setEnabled(false);
                progress.dismiss();
                this.counter = 10;
                break;
            case "PLAYING":
                start.setVisibility(View.GONE);
                start.setEnabled(false);
                play.setVisibility(View.GONE);
                play.setEnabled(false);
                stop.setVisibility(View.VISIBLE);
                stop.setEnabled(true);
                break;
            case "COMPLETED":
                Toast.makeText(context, R.string.completed, Toast.LENGTH_SHORT).show();
                start.setVisibility(View.VISIBLE);
                start.setEnabled(true);
                play.setVisibility(View.GONE);
                play.setEnabled(false);
                stop.setVisibility(View.GONE);
                stop.setEnabled(false);
                transactionId = null;
                this.counter = 10;
                break;
            case "ERROR":
                Toast.makeText(context, R.string.check_connection, Toast.LENGTH_SHORT).show();
                start.setVisibility(View.VISIBLE);
                start.setEnabled(true);
                play.setVisibility(View.GONE);
                play.setEnabled(false);
                stop.setVisibility(View.GONE);
                stop.setEnabled(false);
                this.counter = 10;
                break;
            case "ARCHIVED":
                Toast.makeText(context, R.string.stopped_game, Toast.LENGTH_SHORT).show();
                start.setVisibility(View.VISIBLE);
                start.setEnabled(true);
                play.setVisibility(View.GONE);
                play.setEnabled(false);
                stop.setVisibility(View.GONE);
                stop.setEnabled(false);
                progress.dismiss();
                transactionId = null;
                this.counter = 10;
        }
    }


    private void launchProgressDialog() {
        progress = new ProgressDialog(GameActivity.this, R.style.AppTheme_Dialog);
        progress.setTitle(R.string.creating_connection);
        progress.setMessage("Trying to connect with " + robotIp);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setProgressNumberFormat(null);
        progress.setProgressPercentFormat(null);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();
    }

    private void startPlaying() {
        try {
            Socket client = new Socket();
            client.connect(new InetSocketAddress(robotIp, port), 10000);

            DataOutputStream outToServer = new DataOutputStream(client.getOutputStream());

            String rawJSON = "{\n" +
                    "\"transaction_id\": \"" + transactionId + "\",\n" +
                    "\"job\": \"START\"\n" +
                    "}";

            outToServer.writeUTF(rawJSON);

            DataInputStream in = new DataInputStream(client.getInputStream());
            int responseCode = in.readInt();
            switch (responseCode) {
                case 200:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.started_game, Toast.LENGTH_SHORT).show();
                        }
                    });
                    isStarting = true;
                    startPolling();
                    break;
                case 404:
                case 400:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.cannot_connect, Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
            }
            System.out.println(responseCode);
            client.close();

        } catch(Exception e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, R.string.cannot_connect, Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

}

