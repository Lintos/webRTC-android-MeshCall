/*
 * @author    Lucas Choi <lucas@remotemonster.com>
 * Copyright (c) 2017 RemoteMonster, inc. All Right Reserved.
 */

package com.remotemonster.meshcall;

import android.content.Intent;

/**
 * Class description goes here.
 *
 * @author Lucas Choi <lucas@remotemonster.com>
 * @version 2017-03-07.
 */
public class PermissionReceiverActivity extends PermissionCheckActivity{
    @Override
    public void allPermissionOk() {
        startActivity(new Intent(PermissionReceiverActivity.this, MeshCallActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        if (!isFinishing()) {
            finish();
        }
    }
}