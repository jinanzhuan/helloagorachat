package io.agora.helloagorachat;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.agora.CallBack;
import io.agora.Error;
import io.agora.ValueCallBack;
import io.agora.chat.ChatClient;
import io.agora.chat.ChatMessage;
import io.agora.cloud.HttpClientManager;
import io.agora.cloud.HttpResponse;
import io.agora.helloagorachat.databinding.ActivityMainBinding;
import io.agora.util.EMLog;

public class MainActivity extends AppCompatActivity {
    /**
     * It should be replaced with the url that the developer needs to build by himself.
     */
    private static final String LOGIN_URL = "https://a41.easemob.com/app/chat/user/login";
    /**
     * It should be replaced with the url that the developer needs to build by himself.
     * See: https://docs-preprod.agora.io/en/agora-chat/landing-page?platform=All%20Platforms
     */
    private static final String REGISTER_URL = "https://a41.easemob.com/app/chat/user/register";
    private ActivityMainBinding mainBinding;
    private String username;
    private String password;
    private String toChatName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        initListener();
    }

    private void initListener() {
        mainBinding.btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getUsernameAndPassword();
                if(TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    // Toast
                    return;
                }
                getAgoraChatTokenFromServerAndSignIn();
            }
        });
        mainBinding.btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If you have bind device token, unbindToken should be true
                ChatClient.getInstance().logout(false, new CallBack() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(()-> {
                            Toast.makeText(MainActivity.this, "You have signed out success!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(int code, String error) {
                        EMLog.e("AgoraChat", "Sign out failed, error: "+error);
                    }
                });
            }
        });
        mainBinding.btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getUsernameAndPassword();
                if(TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    // Toast
                    return;
                }
                registerFromAppServer(username, password, new CallBack() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(()-> {
                            Toast.makeText(MainActivity.this, "You have signed up success!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(int code, String error) {
                        EMLog.e("AgoraChat", "Sign up failed, error: "+error);
                    }
                });
            }
        });
        mainBinding.btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!ChatClient.getInstance().isLoggedIn()) {
                    Toast.makeText(MainActivity.this, "You should login to Agora chat first", Toast.LENGTH_SHORT).show();
                    return;
                }
                getUsernameAndPassword();
                if(TextUtils.isEmpty(toChatName)) {
                    Toast.makeText(MainActivity.this, "You should input a username to send", Toast.LENGTH_SHORT).show();
                    return;
                }
                String content = mainBinding.etContent.getText().toString().trim();
                if(TextUtils.isEmpty(content)) {
                    Toast.makeText(MainActivity.this, "You should input something before sending message", Toast.LENGTH_SHORT).show();
                    return;
                }
                ChatMessage message = ChatMessage.createTextSendMessage(content, toChatName);
                message.setAttribute("is_expend", true);
                // you can set a callback to listen the result
                // If you are concerned about the result of sending, you need to
                // set the callback before calling sendMessage
                message.setMessageStatusCallback(new CallBack() {
                    @Override
                    public void onSuccess() {
                        EMLog.e("message", "Send message success");
                    }

                    @Override
                    public void onError(int code, String error) {
                        EMLog.e("message", "Send message failed, error: "+error);
                    }
                });
                ChatClient.getInstance().chatManager().sendMessage(message);
            }
        });
    }

    private void getAgoraChatTokenFromServerAndSignIn() {
        getAgoraChatTokenFromServer(username, password, new ValueCallBack<String>() {
            @Override
            public void onSuccess(String token) {
                ChatClient.getInstance().loginWithAgoraToken(username, token, new CallBack() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(()-> {
                            Toast.makeText(MainActivity.this, "Sign in success!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(int code, String error) {
                        EMLog.e("AgoraChat", "Sign in failed, error: "+error);
                    }
                });
            }

            @Override
            public void onError(int error, String errorMsg) {
                EMLog.e("AgoraChat", "Get AgoraToken failed, error: "+error);
            }
        });
    }

    private void getAgoraChatTokenFromServer(String username, String password, ValueCallBack<String> callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    JSONObject request = new JSONObject();
                    request.putOpt("userAccount", username);
                    request.putOpt("userPassword", password);
                    HttpResponse response = HttpClientManager.httpExecute(LOGIN_URL, headers, request.toString(), "POST");
                    int code = response.code;
                    String responseInfo = response.content;
                    if (code == 200) {
                        if (responseInfo != null && responseInfo.length() > 0) {
                            JSONObject object = new JSONObject(responseInfo);
                            String token = object.getString("accessToken");
                            if(callBack != null) {
                                callBack.onSuccess(token);
                            }
                        } else {
                            if(callBack != null) {
                                callBack.onError(Error.SERVER_UNKNOWN_ERROR, responseInfo);
                            }
                        }
                    } else {
                        if(callBack != null) {
                            callBack.onError(code, responseInfo);
                        }
                    }
                } catch (Exception e) {
                    if(callBack != null) {
                        callBack.onError(Error.GENERAL_ERROR, e.getMessage());
                    }
                }
            }
        }).start();
    }

    private void registerFromAppServer(String username, String password, CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    JSONObject request = new JSONObject();
                    request.putOpt("userAccount", username);
                    request.putOpt("userPassword", password);
                    HttpResponse response = HttpClientManager.httpExecute(REGISTER_URL, headers, request.toString(), "POST");
                    int code=  response.code;
                    String responseInfo = response.content;
                    if (code == 200) {
                        if (responseInfo != null && responseInfo.length() > 0) {
                            JSONObject object = new JSONObject(responseInfo);
                            String resultCode = object.getString("code");
                            if(resultCode.equals("RES_OK")) {
                                if(callBack != null) {
                                    callBack.onSuccess();
                                }
                            }else{
                                if(callBack != null) {
                                    callBack.onError(Error.GENERAL_ERROR, object.getString("errorInfo"));
                                }
                            }
                        } else {
                            if(callBack != null) {
                                callBack.onError(code, responseInfo);
                            }
                        }
                    } else {
                        if(callBack != null) {
                            callBack.onError(code, responseInfo);
                        }
                    }
                } catch (Exception e) {
                    if(callBack != null) {
                        callBack.onError(Error.GENERAL_ERROR, e.getMessage());
                    }
                }
            }
        }).start();
    }

    private void getUsernameAndPassword() {
        username = mainBinding.etUsername.getText().toString().trim();
        password = mainBinding.etPassword.getText().toString().trim();
        toChatName = mainBinding.etToUsername.getText().toString().trim();
    }
}