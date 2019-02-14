/*
 * @author    Lucas Choi <lucas@remotemonster.com>
 * Copyright (c) 2017 RemoteMonster, inc. All Right Reserved.
 */

package com.remotemonster.meshcall;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

/**
 * Manifest에 명시된 퍼미션 리스트 중 안드로이드 6.0(M)부터 사용자 확인을 받아야 하는 위험 퍼미션 리스트를 추려서 사용자에게 요청한다.
 * 필요한 퍼미션을 사용자가 모두 허용했을 경우에만 자식클래스가 구현하는 allPermissionOk() 함수를 호출하고, 그렇지 않을 경우 앱을 종료한다.
 * <p>
 * <p>
 * public class SplashActivity extends PermissionCheckActivity {
 *
 * @Override public void allPermissionOk() {
 * //모든 퍼미션이 허용되었을 경우 본 시점으로 넘어옴.
 * //이 곳에서 필요한 작업 처리
 * }
 * @Override public void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);
 * setContentView(R.layout.activity_splash);
 * <p>
 * //Do not Anything.
 * //사용자가 작업에 필요한 퍼미션을 허용했을지 안했을지 아직 확실치 않으므로
 * //이 시점에서는 아무 작업도 수행해서는 안 됨.
 * //죽는수가 있음. 앱이.
 * }
 * }
 */

public abstract class PermissionCheckActivity extends AppCompatActivity {
    /**
     * on*Result 콜백함수에서 내가 요청한 작업을 구분하기 위한 요청코드
     */
    private final int PERMISSION_REQUEST_CODE = 5900;
    private final int SETTING_REQUEST_CODE = 6000;
    /**
     * 안드로이드 M부터 사용자에게 확인받아야 하는 퍼미션 리스트
     * Ref. https://developer.android.com/guide/topics/security/permissions.html?hl=ko#normal-dangerous
     */
    private final String[] mDangerousPermissions = {
            /* CALENDAR Group */
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,

            /* CAMERA Group */
            Manifest.permission.CAMERA,

            /* CONTACTS Group */
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,

            /* LOCATION Group */
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,

            /* MICROPHONE Group */
            Manifest.permission.RECORD_AUDIO,

            /* PHONE Group */
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.ADD_VOICEMAIL,
            Manifest.permission.USE_SIP,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY,

            /* SENSORS Group */
            Manifest.permission.BODY_SENSORS,

            /* SMS Group */
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.RECEIVE_MMS,

            /* STORAGE Group */
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

    };

    /**
     * 앱 Manifest에서 요청하고 있는 퍼미션 중 사용자 확인을 받아야하는 퍼미션 리스트
     */
    private PermissionCheckList mPermissionCheckList;

    /**
     * 사용자가 [설정]에서 앱에 대한 권한을 수정한 뒤 앱으로 돌아올 경우 onCreate() -> onActivityResult() 순으로 호출됨.
     * 사용자가 [설정]에서 권한을 수정하지 않고 앱으로 돌아올 경우 onActivity()만 호출됨
     * <p>
     * onCreate() 이후 onActivityResult()가 호출될 경우 ActivityCompat.requestPermissions()가 두 번 호출되어 에러를 유발한다.
     * (사용자가 퍼미션 요청 대화상자를 완료하지 않았는데도 onRequestPermissionsResult()가 호출 됨,
     * 이 때 grantResults엔 아무 값도 들어있지 않아 allPermissionOk()까지 호출되는 에러)
     * <p>
     * 이를 막기 위해 아래의 플래그로 onActivityResult()의 작업을 제어한다.
     */
    private boolean isEnabledOnActivityResultProcess;


    /**
     * 자식 클래스가 반드시 구현(Implementation)해야 하는 순수가상함수
     * 사용자가 모든 퍼미션을 활성화 했을 때에만 호출된다.
     * <p>
     * 자식 Activity 클래스는 onCreate에서 아무 작업도 해서는 안되고,
     * 아래 순수가상함수에서만 앱처리 작업을 수행해야 함
     */
    public abstract void allPermissionOk();


    /**
     * onCreate. Manifest에 명시된 퍼미션 중 위험 퍼미션을 찾아 사용자에게 요청한다.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * onCreate() 이후 호출되는 onActivityResult()에서는 ActivityCompat.requestPermissions()를
         * 호출하지 않도록 제어 플래그를 세팅한다.
         */
        isEnabledOnActivityResultProcess = false;

        /** SDK 버전이 Android 6.0(M) 이상일 때만 퍼미션 체크로직 수행 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            mPermissionCheckList = new PermissionCheckList();
            String[] permissionsOnManifest = new String[0];

            /** Get Permissions on the manifest */
            try {
                PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
                permissionsOnManifest = info.requestedPermissions;
            } catch (PackageManager.NameNotFoundException e) {
//                Crashlytics.logException(e);
            }


            /** Find Dangerous Permissions from The manifest's permissions */
            for (String permission : permissionsOnManifest) {
                for (String dangerPermission : mDangerousPermissions) {
                    if (permission.equalsIgnoreCase(dangerPermission)) {
                        mPermissionCheckList.add(permission);
                        break;
                    }
                }
            }

            /** 사용자에게 퍼미션 요청 */
            mPermissionCheckList.check(PermissionCheckActivity.this);

        } else {
            /** SDK 버전이 Android 6.0(M) 미만일 경우 그냥 퍼미션 ok! */
            allPermissionOk();
        }
    }


    /**
     * ActivityCompat.requestPermissions()를 호출하면 사용자에게 대화상자가 팝업되고,
     * 이후 사용자의 권한 허용/거부 상태를 반환해주는 콜백함수
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:

                /** 요청한 퍼미션 중 사용자가 하나라도 '거부'를 했다면 */
                // if (Arrays.binarySearch(grantResults, PackageManager.PERMISSION_DENIED) >= 0)
                // 위 조건식에서 binarySearch() 함수가 이상하다.
                // index 2번에 PackageManager.PERMISSION_DENIED가 있는데 -1를 리턴한다.
                // 그래서 그냥 수동으로 찾는다.
                boolean isDenied = false;
                for (int item : grantResults) {
                    if (item == PackageManager.PERMISSION_DENIED) {
                        isDenied = true;
                        break;
                    }
                }

                /** 거부했다면 [설정]>[권한]에서 수동으로 권한을 설정해달라는 다이얼로그 보여주자 */
                if (isDenied) {

                    /**
                     * [설정]에서 앱으로 돌아왔을 때 onCreate()가 호출되지 않았다면 onActivityResult()에서
                     * ActivityCompat.requestPermissions()를 호출할 수 있도록 제어 플래그를 해제한다.
                     */
                    isEnabledOnActivityResultProcess = true;

                    showRequestEnablePermissionDialog();

                } else { /** 모두 허용했다면 자식 클래스가 정의한 다음작업 수행 */
                    allPermissionOk();
                }

                break;

            default:
                //nop
        }
    }

    private void showRequestEnablePermissionDialog() {
        new AlertDialog.Builder(PermissionCheckActivity.this)
                .setTitle(R.string.permission_info_dlg_title)
                .setMessage(R.string.permission_info_dlg_message)
                .setCancelable(false)
                .setPositiveButton(R.string.permission_deny_dlg_btn_setting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        /** 앱 설정 페이지로 이동 */
                        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, SETTING_REQUEST_CODE);
                    }
                })
                .setNegativeButton(R.string.permission_deny_dlg_btn_close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        /** 그럼에도 그냥 무시했다면 앱 종료 */
                        if (!isFinishing()) {
                            finish();
                        }
                    }
                }).show();
    }

    /**
     * 앱 설정 페이지에서 앱으로 돌아왔을 때의 작업 처리
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SETTING_REQUEST_CODE:

                /** 제어 플래그가 클리어되었다면 ActivityCompat.requestPermissions()를 호출한다. */
                if (isEnabledOnActivityResultProcess) {
                    mPermissionCheckList.check(PermissionCheckActivity.this);
                }
                break;
        }
    }


    /**
     * 한 번에 requestPermissions()를 호출해주는 기능을 가진 ArrayList<String>
     */
    private class PermissionCheckList extends ArrayList<String> {
        /**
         * Add 된 퍼미션들을 중 Granted 되지 않는 항목을 찾아 requestPermissions()를 호출한다.
         *
         * @param activity
         */
        public void check(final Activity activity) {

            /** Granted 되지 않은 퍼미션을 찾아라 */
            final ArrayList<String> permissions = new ArrayList<>();
            for (String item : this) {
                if (ContextCompat.checkSelfPermission((activity), item) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(item);
                }
            }

            /** Granted 되지 않은 퍼미션이 있다면 요청하고, 없다면 자식 클랙스가 정의한 다음작업 수행 */
            final int size = permissions.size();
            if (size == 0) {
                allPermissionOk();
            } else {
                new AlertDialog.Builder(PermissionCheckActivity.this)
                        .setTitle(R.string.permission_info_dlg_title)
                        .setMessage(R.string.permission_info_dlg_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.permission_info_dlg_message_enter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                String[] arr = new String[size];
                                ActivityCompat.requestPermissions(activity, permissions.toArray(arr), PERMISSION_REQUEST_CODE);
                            }
                        }).show();
            }
        }
    }
}