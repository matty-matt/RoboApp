package roboniania.com.roboniania_android.api.network;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import roboniania.com.roboniania_android.R;
import roboniania.com.roboniania_android.api.RoboService;
import roboniania.com.roboniania_android.api.model.OAuthToken;
import roboniania.com.roboniania_android.api.model.Oauth2;
import roboniania.com.roboniania_android.api.model.Robot;
import roboniania.com.roboniania_android.api.model.User;
import roboniania.com.roboniania_android.storage.SharedPreferenceStorage;

public class NetworkProvider {

    private final Context context;
    private SharedPreferenceStorage userLocalStorage;

    private final List<User> users = new ArrayList<>();
    private final List<Robot> robots = new ArrayList<>();

    private static final String login_code = "login_code";
    private static final String robot_pair = "robot_pair";
    private static final String robot_list = "robot_list";
    private int RESPONSE_CODE;


    public NetworkProvider(Context context, SharedPreferenceStorage userLocalStorage) {
        this.context = context;
        this.userLocalStorage = userLocalStorage;
    }

    public List<Robot> getRobots() {
        return robots;
    }

    public int getRESPONSE_CODE() {
        return RESPONSE_CODE;
    }

    public interface OnResponseReceivedListener {
        void onResponseReceived();
    }

    private Robot parseRobot(String robotString) throws JSONException {
        JSONObject recipeObject = new JSONObject(robotString);

        Robot robot = new Robot(recipeObject.getString("ip"),
                    recipeObject.getString("sn"),
                    recipeObject.getString("uuid"));

            return robot;

    }

    public void pair(String pairKey, OnResponseReceivedListener listener) throws IOException, JSONException {
        if (isOnline()) {
            String s = checkPairKey(pairKey);
            if (s == null) {
                // INVALID PAIRKEY
            } else {
                // UNUSED RETURNED VALUE ROBOT
                parseRobot(s);
            }

        } else {
            // NO INTERNET
        }
        listener.onResponseReceived();
    }

    private String checkPairKey(String pairKey) throws IOException {
        NetworkRequest request = new NetworkRequest(RoboService.ROBOTS_PAIR, HttpMethod.GET, null, userLocalStorage, pairKey, robot_pair);
        String response = request.execute();
        RESPONSE_CODE = request.getRESPONSE_CODE();
        System.out.println("RESPONSE CODE IN PROVIDER : " + RESPONSE_CODE);

        if (RESPONSE_CODE == 200 || RESPONSE_CODE == 202) {
            // SUCCESSFULLY PAIRED
            return response;
        }
        else {
            // INVALID PAIRKEY
            return null;
            //TODO catch code error
        }
    }

    private boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public void login(String login, String password, OnResponseReceivedListener listener) throws IOException, JSONException {
        NetworkRequest request = new NetworkRequest(RoboService.OAUTH2 + "?grant_type=password&username="+login+"&password="+password, HttpMethod.POST, null, login_code);
        String response = request.execute();
        RESPONSE_CODE = request.getRESPONSE_CODE();

        if (RESPONSE_CODE == 200 || RESPONSE_CODE == 202) {
            Oauth2 oauth2 = parseToken(response);

            // Save token to shared prefernces
            userLocalStorage.storeAccessToken(oauth2.getAccess_token());
            userLocalStorage.setUserLoggedIn(true);
            System.out.println("TOKEN: " + oauth2.getAccess_token());
        }
        listener.onResponseReceived();
    }

    private Oauth2 parseToken(String response) throws  JSONException {
        JSONObject responseObject = new JSONObject(response);
        Oauth2 token = new Oauth2(responseObject.getString("access_token"),
                responseObject.getString("token_type"),
                responseObject.getString("refresh_token"),
                responseObject.getInt("expires_in"),
                responseObject.getString("scope"));
        return token;
    }

    public void getRobotList(OnResponseReceivedListener listener) throws IOException, JSONException {
        NetworkRequest request = new NetworkRequest(RoboService.ROBOTS_LIST, HttpMethod.GET, null, userLocalStorage, robot_list);
        String response = request.execute();
        RESPONSE_CODE = request.getRESPONSE_CODE();

        if (RESPONSE_CODE == 200 || RESPONSE_CODE == 202) {
            //USER CAN BE USED LATER
            User user = parseUser(response);

            for (Robot robot : robots) {
                System.out.println(robot.getIp() + " || " + robot.getSn() + " || " + robot.getUuid());
            }

        } else {
            //PROBLEM WITH FETCHING ROBOT LIST
        }
        listener.onResponseReceived();
    }

    private User parseUser(String response) throws JSONException {
        JSONObject responseObject = new JSONObject(response);
        User user = new User();
        user.setLogin(responseObject.getString("login"));
        user.setCreateed(responseObject.getString("createed"));
        user.setPassword(responseObject.getString("password"));
        user.setToken(responseObject.getString("token"));
        user.setUserId(responseObject.getString("userId"));

        JSONArray robotArray = responseObject.getJSONArray("robots");
        for (int i = 0; i < robotArray.length(); ++i) {
            JSONObject robotObj = robotArray.getJSONObject(i);

            Robot robot = new Robot(robotObj.getString("ip"),
                    robotObj.getString("sn"),
                    robotObj.getString("uuid"));

            robots.add(robot);
        }

        user.setRobots(robots);

        return user;
    }
}