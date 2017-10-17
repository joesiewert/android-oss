package com.kickstarter.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.kickstarter.libs.BaseActivity;
import com.kickstarter.libs.RefTag;
import com.kickstarter.libs.qualifiers.RequiresActivityViewModel;
import com.kickstarter.libs.utils.ApplicationUtils;
import com.kickstarter.ui.IntentKey;
import com.kickstarter.viewmodels.DeepLinkViewModel;

import java.util.ArrayList;
import java.util.List;

import static com.kickstarter.libs.rx.transformers.Transformers.observeForUI;

@RequiresActivityViewModel(DeepLinkViewModel.ViewModel.class)
public class DeepLinkActivity extends BaseActivity<DeepLinkViewModel.ViewModel> {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.viewModel.outputs.startDiscoveryActivity()
      .compose(bindToLifecycle())
      .compose(observeForUI())
      .subscribe(__ -> startDiscoveryActivity());

    this.viewModel.outputs.startProjectActivity()
      .compose(bindToLifecycle())
      .compose(observeForUI())
      .subscribe(this::startProjectActivity);

    this.viewModel.outputs.startBrowser()
      .compose(bindToLifecycle())
      .compose(observeForUI())
      .subscribe(this::startBrowser);
  }

  private void startDiscoveryActivity() {
    ApplicationUtils.startNewDiscoveryActivity(this);
    finish();
  }

  private void startProjectActivity(String url) {
    Uri uri = Uri.parse(url);
    final Intent projectIntent = new Intent(this, ProjectActivity.class)
      .setData(uri);
    String ref = uri.getQueryParameter("ref");
    if(ref != null) {
      projectIntent.putExtra(IntentKey.REF_TAG, RefTag.from(ref));
    }
    startActivity(projectIntent);
    finish();
  }

  private void startBrowser(String url) {
    Uri uri = Uri.parse(url);

    // We'll ask the system to open a generic URL, rather than the deep-link
    // capable one we actually want.
    Uri fakeUri = Uri.parse("http://www.kickstarter.com");

    Intent browserIntent = new Intent(Intent.ACTION_VIEW, fakeUri);
    PackageManager pm = getPackageManager();
    List<ResolveInfo> activities = pm.queryIntentActivities(browserIntent, 0);

    // Loop through everything the system gives us, and remove the current
    // app (the whole point here is to open the link in something else).
    final List<Intent> targetIntents = new ArrayList<>(activities.size());
    for (ResolveInfo currentInfo : activities) {
      String packageName = currentInfo.activityInfo.packageName;
      if (!packageName.contains("com.kickstarter")) {
        // Build an intent pointing to the found package, but
        // this intent will contain the _real_ url.
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        intent.setData(uri);
        targetIntents.add(intent);
      }
    }

    // Now present the user with the list of apps we have found (this chooser
    // is smart enough to just open a single option directly, so we don't need
    // to handle that case).
    Intent chooserIntent = Intent.createChooser(targetIntents.remove(0), "");
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
      targetIntents.toArray(new Parcelable[targetIntents.size()]));
    startActivity(chooserIntent);

    finish();
  }
}