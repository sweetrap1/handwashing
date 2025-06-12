package com.jarindimick.handwashtracking.gui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Import for @Nullable annotation
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat; // Essential for parsing HTML in TextView
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.jarindimick.handwashtracking.R;
import com.jarindimick.handwashtracking.databasehelper.DatabaseHelper;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// AdMob Imports
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

// Google Play Billing Imports
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.google.common.collect.ImmutableList; // Requires Guava if not already imported

public class MainHandwashing extends AppCompatActivity implements PurchasesUpdatedListener {

    private ImageView img_custom_logo;
    private TextView txt_datetime;
    private EditText edit_employee_number;
    private Button btn_start;
    private TableLayout table_top_handwashers;
    private final Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateTimeRunnable;
    private DatabaseHelper dbHelper;
    private Toolbar mainToolbar;
    private AdView mAdView;
    private Button btn_upgrade_to_pro; // New button for upgrade

    // Executor for background tasks
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    public static final String PREFS_MAIN_TOUR_FILE = "MainTourPrefs";
    public static final String KEY_MAIN_INTERACTIVE_TOUR_SHOWN = "mainInteractiveTourShown";
    public static final String PREFS_APP_SETTINGS = "AppSettings";
    public static final String KEY_IS_PRO_VERSION = "isProVersion";

    private boolean hasWindowFocusForTour = false;
    private boolean interactiveTourAttemptedThisSession = false;

    // A flag to check if the current build flavor is "free"
    private boolean isFreeVersion;
    private boolean isProVersionUnlocked = false; // Tracks if Pro features are unlocked via IAP

    // BillingClient instance
    private BillingClient billingClient;
    // SKU ID for your Pro version upgrade. You MUST replace this with the actual product ID from Play Console.
    private static final String PRODUCT_ID_PRO_UPGRADE = "handwash_pro_upgrade";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_handwashing);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        // Determine if this is the free version based on the build flavor
        isFreeVersion = getApplicationContext().getPackageName().endsWith(".free");
        Log.d("MainHandwashing", "isFreeVersion: " + isFreeVersion);

        // Load Pro status from SharedPreferences
        SharedPreferences appSettings = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
        isProVersionUnlocked = appSettings.getBoolean(KEY_IS_PRO_VERSION, false);
        Log.d("MainHandwashing", "isProVersionUnlocked: " + isProVersionUnlocked);


        setupgui();

        if (mainToolbar != null) {
            setSupportActionBar(mainToolbar);
        }

        setupListeners();
        // Initialize DatabaseHelper with the isFreeVersion flag.
        // If it's the free flavor AND pro version is NOT unlocked, then it acts as free.
        // Otherwise, it acts as pro (either pro flavor or free flavor with IAP unlocked).
        dbHelper = new DatabaseHelper(this, isFreeVersion && !isProVersionUnlocked);

        // Initialize Mobile Ads SDK and load ad if it's the free version AND pro version is NOT unlocked
        if (isFreeVersion && !isProVersionUnlocked) {
            initializeMobileAds();
        } else if (mAdView != null) {
            mAdView.setVisibility(View.GONE); // Hide ad if it's pro version or free version with IAP
        }

        // Setup BillingClient if it's the free version
        if (isFreeVersion) {
            setupBillingClient();
        } else {
            // Hide upgrade button for Pro version
            if (btn_upgrade_to_pro != null) {
                btn_upgrade_to_pro.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !interactiveTourAttemptedThisSession) {
            hasWindowFocusForTour = true;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (hasWindowFocusForTour) {
                    showInteractiveTourIfNeeded();
                }
            }, 300);
        } else if (!hasFocus) {
            hasWindowFocusForTour = false;
        }
    }

    private void setupgui() {
        img_custom_logo = findViewById(R.id.img_custom_logo);
        txt_datetime = findViewById(R.id.txt_datetime);
        edit_employee_number = findViewById(R.id.edit_employee_number);
        btn_start = findViewById(R.id.btn_start);
        table_top_handwashers = findViewById(R.id.table_top_handwashers);
        mainToolbar = findViewById(R.id.main_toolbar);
        // Ensure this ID exists in your layout XML
        btn_upgrade_to_pro = findViewById(R.id.btn_upgrade_to_pro);

        // Find the AdView if it exists in the layout
        mAdView = findViewById(R.id.adView);
        // Initial AdView visibility based on whether it's the free version and not unlocked
        if (mAdView != null) {
            mAdView.setVisibility(isFreeVersion && !isProVersionUnlocked ? View.VISIBLE : View.GONE);
        }

        // Set upgrade button visibility
        if (btn_upgrade_to_pro != null) {
            btn_upgrade_to_pro.setVisibility(isFreeVersion && !isProVersionUnlocked ? View.VISIBLE : View.GONE);
        }
    }

    // New method to initialize Mobile Ads and load banner ad
    private void initializeMobileAds() {
        // Set RequestConfiguration to tag for child-directed treatment if applicable
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .build();
        MobileAds.setRequestConfiguration(requestConfiguration);

        MobileAds.initialize(this, initializationStatus -> {
            // Initialization is complete. Now load the ad.
            Log.d("AdMob", "MobileAds initialized: " + initializationStatus.getAdapterStatusMap());

            // Check if the AdView is not null (meaning it exists in the layout for this flavor)
            // The adUnitId is now set directly in XML, so no need to set it here.
            if (mAdView != null) {
                // Request a test ad for development. Remove this line for production.
                AdRequest adRequest = new AdRequest.Builder().build();

                // Load the ad
                mAdView.loadAd(adRequest);
                Log.d("AdMob", "Ad requested for mAdView.");
            } else {
                Log.e("AdMob", "AdView is null, cannot load ad. This might be expected for Pro version.");
            }
        });
    }

    // --- Google Play Billing Methods ---
    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingClient", "Billing setup successful.");
                    queryProductDetails();
                    queryPurchases(); // Check for existing purchases
                } else {
                    Log.e("BillingClient", "Billing setup failed: " + billingResult.getDebugMessage());
                    Toast.makeText(MainHandwashing.this, "Billing not available: " + billingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to Google Play by calling startConnection().
                Log.w("BillingClient", "Billing service disconnected. Will try to reconnect.");
            }
        });

        if (btn_upgrade_to_pro != null) {
            btn_upgrade_to_pro.setOnClickListener(v -> launchPurchaseFlow());
        }
    }

    private void queryProductDetails() {
        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(ImmutableList.of(
                                QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId(PRODUCT_ID_PRO_UPGRADE)
                                        .setProductType(BillingClient.ProductType.INAPP)
                                        .build()))
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                new ProductDetailsResponseListener() {
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult,
                                                         List<ProductDetails> productDetailsList) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                            Log.d("BillingClient", "Product details fetched: " + productDetailsList.size());
                            // Store productDetails for later use in launchPurchaseFlow
                            // For simplicity, we'll assume there's only one product (PRO_UPGRADE)
                            // In a real app, you might want to store this in a member variable.
                            // For now, we'll just log it.
                            for (ProductDetails productDetails : productDetailsList) {
                                Log.d("BillingClient", "Product: " + productDetails.getProductId() + ", Price: " + productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());
                            }
                        } else {
                            Log.e("BillingClient", "Failed to query product details: " + billingResult.getDebugMessage());
                            Toast.makeText(MainHandwashing.this, "Could not fetch product details: " + billingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void launchPurchaseFlow() {
        if (!billingClient.isReady()) {
            Toast.makeText(this, "Billing service not ready. Please try again in a moment.", Toast.LENGTH_SHORT).show();
            setupBillingClient(); // Try to reconnect
            return;
        }

        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(ImmutableList.of(
                                QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId(PRODUCT_ID_PRO_UPGRADE)
                                        .setProductType(BillingClient.ProductType.INAPP)
                                        .build()))
                        .build();

        billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                ProductDetails proUpgradeProduct = productDetailsList.get(0);
                if (proUpgradeProduct.getOneTimePurchaseOfferDetails() != null) {
                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(ImmutableList.of(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(proUpgradeProduct)
                                            // Removed setOfferToken() for one-time purchases as it's not applicable here.
                                            .build()))
                            .build();

                    billingClient.launchBillingFlow(this, billingFlowParams);
                } else {
                    Log.e("BillingClient", "No one-time purchase offer found for PRO_UPGRADE.");
                    Toast.makeText(this, "Product offer not available.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("BillingClient", "Failed to get product details for purchase flow: " + billingResult.getDebugMessage());
                Toast.makeText(this, "Failed to get product details for purchase.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Toast.makeText(this, "Purchase cancelled.", Toast.LENGTH_SHORT).show();
            Log.d("BillingClient", "User cancelled the purchase flow.");
        } else {
            // Handle any other error codes.
            Toast.makeText(this, "Purchase error: " + billingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
            Log.e("BillingClient", "Purchase error: " + billingResult.getDebugMessage());
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getProducts().contains(PRODUCT_ID_PRO_UPGRADE) && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                // For one-time purchases, you usually consume them after granting entitlement.
                // However, first acknowledge if not already acknowledged.
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.d("BillingClient", "Purchase acknowledged. Now consuming for one-time entitlement.");
                        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                        billingClient.consumeAsync(consumeParams, (consumeBillingResult, purchaseToken) -> {
                            if (consumeBillingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                Log.d("BillingClient", "Purchase consumed successfully. Granting entitlement.");
                                Toast.makeText(MainHandwashing.this, "Upgrade to Pro successful! No more ads and all features unlocked.", Toast.LENGTH_LONG).show();
                                grantProVersionEntitlement();
                            } else {
                                Log.e("BillingClient", "Failed to consume purchase: " + consumeBillingResult.getDebugMessage());
                                Toast.makeText(MainHandwashing.this, "Failed to complete purchase: " + consumeBillingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Log.e("BillingClient", "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
                        Toast.makeText(MainHandwashing.this, "Failed to acknowledge purchase: " + billingResult.getDebugMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // Purchase already acknowledged. If it's a one-time purchase, ensure it's consumed.
                // Re-query purchases to ensure we don't try to consume already consumed ones.
                Log.d("BillingClient", "Purchase already acknowledged. Ensuring entitlement is granted.");
                grantProVersionEntitlement(); // Ensure Pro features are active
            }
        } else {
            Log.w("BillingClient", "Purchase is not for Pro upgrade or not purchased: " + purchase.getProducts());
        }
    }

    private void queryPurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        boolean proFound = false;
                        for (Purchase purchase : purchases) {
                            if (purchase.getProducts().contains(PRODUCT_ID_PRO_UPGRADE) &&
                                    purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                Log.d("BillingClient", "Found existing Pro purchase.");
                                proFound = true;
                                // If not acknowledged, acknowledge it. For one-time products, you usually
                                // acknowledge and then consume.
                                if (!purchase.isAcknowledged()) {
                                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                                            AcknowledgePurchaseParams.newBuilder()
                                                    .setPurchaseToken(purchase.getPurchaseToken())
                                                    .build();
                                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult1 -> {
                                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                            Log.d("BillingClient", "Existing purchase acknowledged. Now consuming.");
                                            // After acknowledging, consume it so the user can potentially buy again (for managed products)
                                            // For this "upgrade" which is effectively a one-time purchase, consuming makes sense.
                                            ConsumeParams consumeParams = ConsumeParams.newBuilder()
                                                    .setPurchaseToken(purchase.getPurchaseToken())
                                                    .build();
                                            billingClient.consumeAsync(consumeParams, (consumeBillingResult, s) -> {
                                                if (consumeBillingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                    Log.d("BillingClient", "Existing Pro purchase consumed successfully.");
                                                    grantProVersionEntitlement(); // Grant entitlement after consumption
                                                } else {
                                                    Log.e("BillingClient", "Failed to consume existing Pro purchase: " + consumeBillingResult.getDebugMessage());
                                                }
                                            });
                                        } else {
                                            Log.e("BillingClient", "Failed to acknowledge existing Pro purchase: " + billingResult1.getDebugMessage());
                                        }
                                    });
                                } else {
                                    // If already acknowledged, grant entitlement (it should have been consumed already if it's a one-time)
                                    Log.d("BillingClient", "Existing Pro purchase already acknowledged. Granting entitlement.");
                                    grantProVersionEntitlement();
                                }
                                break; // Stop checking after finding the Pro purchase
                            }
                        }
                        if (!proFound) {
                            // If loop finishes and no Pro purchase found, ensure Pro features are off
                            if (isProVersionUnlocked) {
                                revokeProVersionEntitlement();
                            }
                        }
                    } else {
                        Log.e("BillingClient", "Failed to query purchases: " + billingResult.getDebugMessage());
                    }
                }
        );
    }


    private void grantProVersionEntitlement() {
        if (!isProVersionUnlocked) {
            isProVersionUnlocked = true;
            SharedPreferences appSettings = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
            SharedPreferences.Editor editor = appSettings.edit();
            editor.putBoolean(KEY_IS_PRO_VERSION, true);
            editor.apply();
            Log.d("ProVersion", "Pro version granted and saved.");

            // Apply Pro features: Hide ads, re-initialize DBHelper with pro logic, etc.
            if (mAdView != null) {
                mAdView.setVisibility(View.GONE);
                mAdView.destroy(); // Destroy ad resources
            }
            if (btn_upgrade_to_pro != null) {
                btn_upgrade_to_pro.setVisibility(View.GONE);
            }
            // Re-initialize dbHelper to use Pro logic (no employee limit)
            dbHelper = new DatabaseHelper(this, false); // false = not free version
            Toast.makeText(this, "Pro features activated!", Toast.LENGTH_SHORT).show();
            // You might want to refresh UI elements that depend on Pro status here
            // e.g., if there's a "Pro" badge, hide it.
        }
    }

    private void revokeProVersionEntitlement() {
        if (isProVersionUnlocked) {
            isProVersionUnlocked = false;
            SharedPreferences appSettings = getSharedPreferences(PREFS_APP_SETTINGS, MODE_PRIVATE);
            SharedPreferences.Editor editor = appSettings.edit();
            editor.putBoolean(KEY_IS_PRO_VERSION, false);
            editor.apply();
            Log.d("ProVersion", "Pro version revoked and saved.");

            // Apply Free features: show ads, re-initialize DBHelper with free logic, etc.
            if (isFreeVersion) { // Only re-show ads if it's the free flavor
                if (mAdView != null) {
                    mAdView.setVisibility(View.VISIBLE);
                    initializeMobileAds(); // Re-initialize to show ads
                }
                if (btn_upgrade_to_pro != null) {
                    btn_upgrade_to_pro.setVisibility(View.VISIBLE);
                }
            }
            // Re-initialize dbHelper to use Free logic (employee limit)
            dbHelper = new DatabaseHelper(this, true); // true = free version
            Toast.makeText(this, "Pro features revoked. App is now Free version.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Existing methods from your code (continued) ---

    private void showInteractiveTourIfNeeded() {
        final SharedPreferences tourPrefs = getSharedPreferences(PREFS_MAIN_TOUR_FILE, MODE_PRIVATE);
        boolean tourShown = tourPrefs.getBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, false);

        if (tourShown || interactiveTourAttemptedThisSession) {
            return;
        }

        interactiveTourAttemptedThisSession = true;

        // Only show tour if not Pro version unlocked (so they see the Pro upgrade option)
        if (isFreeVersion && !isProVersionUnlocked && edit_employee_number != null && mainToolbar != null && mainToolbar.getMenu() != null && mainToolbar.getMenu().findItem(R.id.menu_admin_login) != null) {
            String adminTitle = "Admin Features";
            String adminDescription = "Access employee management & app settings. Default login and password: admin (Change password later)";

            TapTargetSequence.Listener sequenceListener = new TapTargetSequence.Listener() {
                @Override
                public void onSequenceFinish() {
                    showFreeVersionLimitationsDialog(() -> {
                        SharedPreferences.Editor editor = tourPrefs.edit();
                        editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                        editor.apply();
                    });
                }

                @Override
                public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {}

                @Override
                public void onSequenceCanceled(TapTarget lastTarget) {
                    showFreeVersionLimitationsDialog(() -> {
                        SharedPreferences.Editor editor = tourPrefs.edit();
                        editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                        editor.apply();
                    });
                }
            };

            ArrayList<TapTarget> targets = new ArrayList<>();
            targets.add(TapTarget.forView(edit_employee_number, "Enter employee number here!", "Enter the number 0 to start a Handwash for a unregistered user.")
                    .outerCircleColor(R.color.purple_500).outerCircleAlpha(0.75f).targetCircleColor(android.R.color.white)
                    .titleTextColor(android.R.color.white).descriptionTextColor(android.R.color.white)
                    .textTypeface(Typeface.SANS_SERIF).dimColor(R.color.tour_dim_background).drawShadow(true)
                    .cancelable(false).targetRadius(20).id(1));

            try {
                targets.add(TapTarget.forToolbarOverflow(mainToolbar, adminTitle, adminDescription)
                        .outerCircleColor(R.color.purple_500).outerCircleAlpha(0.75f).targetCircleColor(android.R.color.white)
                        .titleTextColor(android.R.color.white).descriptionTextColor(android.R.color.white)
                        .textTypeface(Typeface.SANS_SERIF).dimColor(R.color.tour_dim_background).drawShadow(true)
                        .cancelable(false).targetRadius(20).id(2));
            } catch (Exception e) {
                Log.e("TapTarget", "Failed to create TapTarget for toolbar overflow.", e);
                sequenceListener.onSequenceCanceled(null);
                return;
            }

            if (targets.size() == 2) {
                new TapTargetSequence(this).targets(targets).listener(sequenceListener).continueOnCancel(false).start();
            } else {
                showFreeVersionLimitationsDialog(() -> {
                    SharedPreferences.Editor editor = tourPrefs.edit();
                    editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
                    editor.apply();
                });
            }
        } else {
            // If it's the Pro version, just mark the tour as shown to prevent it from ever showing
            SharedPreferences.Editor editor = tourPrefs.edit();
            editor.putBoolean(KEY_MAIN_INTERACTIVE_TOUR_SHOWN, true);
            editor.apply();
            // Still show limitations dialog if it's the free version and tour wasn't shown
            if (isFreeVersion && !tourShown) { // Only if not shown AND it's free
                showFreeVersionLimitationsDialog(null); // Pass null listener as tour is already marked shown
            }
        }
    }

    interface OnLimitationsDialogDismissed {
        void onDismissed();
    }

    private void showFreeVersionLimitationsDialog(OnLimitationsDialogDismissed onDismissedListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainHandwashing.this);
        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate our NEW, simpler layout file made specifically for the dialog.
        View dialogView = inflater.inflate(R.layout.dialog_welcome_content, null);

        // Find the TextView for the full message and set its text using HtmlCompat
        TextView dialogMessageFull = dialogView.findViewById(R.id.dialog_message_full);
        if (dialogMessageFull != null) {
            dialogMessageFull.setText(HtmlCompat.fromHtml(getString(R.string.dialog_welcome_message_updated), HtmlCompat.FROM_HTML_MODE_LEGACY));
        }

        // Set the custom view and a standard positive button. This is a more reliable way.
        builder.setView(dialogView)
                .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
                    // This code runs when the "OK, Got It!" button is clicked.
                    dialog.dismiss();
                    if (onDismissedListener != null) {
                        onDismissedListener.onDismissed();
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // User must click the button to dismiss.

        // Show the dialog, but only if the activity is still running.
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadLeaderboardDataAsync();
        loadCustomLogoAsync(); // Use the new asynchronous method
        interactiveTourAttemptedThisSession = false;
        hasWindowFocusForTour = false;

        // Resume ad if it's the free version and ad is present AND Pro not unlocked
        if (isFreeVersion && !isProVersionUnlocked && mAdView != null) {
            mAdView.resume();
        }
        // Re-query purchases to ensure current Pro status is reflected
        if (isFreeVersion && billingClient != null && billingClient.isReady()) {
            queryPurchases();
        }
    }

    @Override
    protected void onPause() {
        // Pause ad if it's the free version and ad is present AND Pro not unlocked
        if (isFreeVersion && !isProVersionUnlocked && mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Destroy ad if it's the free version and ad is present AND Pro not unlocked
        if (isFreeVersion && !isProVersionUnlocked && mAdView != null) {
            mAdView.destroy();
        }
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
        super.onDestroy();
        if (timeUpdateHandler != null && updateTimeRunnable != null) {
            timeUpdateHandler.removeCallbacks(updateTimeRunnable);
        }
        backgroundExecutor.shutdown();
    }


    private void loadCustomLogoAsync() {
        backgroundExecutor.execute(() -> {
            SharedPreferences prefs = getSharedPreferences(AdminDashboardActivity.PREFS_NAME, MODE_PRIVATE);
            String logoPath = prefs.getString(AdminDashboardActivity.KEY_CUSTOM_LOGO_PATH, null);

            final Bitmap logoBitmap;
            if (logoPath != null) {
                File logoFile = new File(logoPath);
                if (logoFile.exists()) {
                    // This is the slow part that is now in the background
                    logoBitmap = BitmapFactory.decodeFile(logoFile.getAbsolutePath());
                } else {
                    logoBitmap = null;
                }
            } else {
                logoBitmap = null;
            }

            // Update the UI on the main thread
            runOnUiThread(() -> {
                if (img_custom_logo != null) {
                    if (logoBitmap != null) {
                        img_custom_logo.setImageBitmap(logoBitmap);
                        img_custom_logo.setVisibility(View.VISIBLE);
                    } else {
                        img_custom_logo.setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private void setupListeners() {
        if (btn_start == null || edit_employee_number == null) return;

        btn_start.setOnClickListener(v -> {
            String employeeNumberStr = edit_employee_number.getText().toString().trim();
            edit_employee_number.setError(null);

            if (employeeNumberStr.isEmpty()) {
                edit_employee_number.setError("Please enter employee number");
                edit_employee_number.requestFocus();
                return;
            }

            backgroundExecutor.execute(() -> {
                boolean employeeExists = dbHelper.doesEmployeeExist(employeeNumberStr);
                runOnUiThread(() -> {
                    if (employeeExists) {
                        Intent intent = new Intent(MainHandwashing.this, WetHandsActivity.class);
                        intent.putExtra("employee_number", employeeNumberStr);
                        intent.putExtra("overall_time_remaining", WetHandsActivity.TOTAL_PROCESS_DURATION_MS);
                        startActivity(intent);
                        edit_employee_number.setText("");
                    } else {
                        edit_employee_number.setError("Employee number not found");
                        edit_employee_number.requestFocus();
                    }
                });
            });
        });

        edit_employee_number.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                if (btn_start != null) {
                    btn_start.performClick();
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Hide admin login for free version (if desired, or offer upgrade)
        // For now, we'll keep it visible, but this is where you could add logic:
        // if (isFreeVersion) {
        //     MenuItem adminLoginItem = menu.findItem(R.id.menu_admin_login);
        //     if (adminLoginItem != null) {
        //         adminLoginItem.setVisible(false);
        //     }
        // }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_admin_login) {
            Intent intent = new Intent(MainHandwashing.this, AdminLoginActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadLeaderboardDataAsync() {
        if (table_top_handwashers == null || dbHelper == null) return;

        table_top_handwashers.removeAllViews();
        TableRow loadingRow = new TableRow(this);
        TextView loadingMsg = new TextView(this);
        loadingMsg.setText("Loading leaderboard...");
        loadingMsg.setGravity(Gravity.CENTER);
        TableRow.LayoutParams loadingParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        loadingParams.span = 3;
        loadingRow.addView(loadingMsg, loadingParams);
        table_top_handwashers.addView(loadingRow);

        backgroundExecutor.execute(() -> {
            List<LeaderboardEntry> leaderboardData = dbHelper.getTopHandwashers();
            runOnUiThread(() -> populateLeaderboardTable(leaderboardData));
        });
    }

    private void populateLeaderboardTable(List<LeaderboardEntry> leaderboardData) {
        if (table_top_handwashers == null) return;

        table_top_handwashers.removeAllViews();
        TableRow headerRow = new TableRow(this);
        headerRow.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));

        TextView rankHeader = createTableHeaderTextView("Rank");
        TextView nameHeader = createTableHeaderTextView("Name");
        TextView countHeader = createTableHeaderTextView("Washes");

        rankHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f));
        nameHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f));
        countHeader.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));

        headerRow.addView(rankHeader);
        headerRow.addView(nameHeader);
        headerRow.addView(countHeader);
        table_top_handwashers.addView(headerRow);

        if (leaderboardData.isEmpty()) {
            TableRow emptyRow = new TableRow(this);
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No handwashes recorded yet today!");
            emptyMsg.setTextSize(16);
            emptyMsg.setPadding(dpToPx(8), dpToPx(16), dpToPx(8), dpToPx(16));
            emptyMsg.setGravity(Gravity.CENTER);
            TableRow.LayoutParams emptyParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            emptyParams.span = 3;
            emptyRow.addView(emptyMsg, emptyParams);
            table_top_handwashers.addView(emptyRow);
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : leaderboardData) {
                TableRow dataRow = new TableRow(this);
                dataRow.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));

                TextView rankView = createDataTextView(String.valueOf(rank) + ".", 20, Typeface.BOLD);
                rankView.setGravity(Gravity.CENTER);

                LinearLayout nameCellLayout = new LinearLayout(this);
                nameCellLayout.setOrientation(LinearLayout.HORIZONTAL);
                nameCellLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                ImageView starIcon = new ImageView(this);
                starIcon.setImageResource(R.drawable.ic_star_leaderboard);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
                iconParams.setMarginEnd(dpToPx(4));
                starIcon.setLayoutParams(iconParams);

                String firstName = entry.employeeName;
                String lastName = entry.lastName;
                String displayName = (firstName == null) ? "" : firstName;
                if (lastName != null && !lastName.isEmpty()) {
                    displayName = displayName.trim() + " " + lastName.charAt(0) + ".";
                } else if (firstName != null && firstName.equals("Guest") && (lastName == null || lastName.isEmpty())) {
                    displayName = "Guest";
                }
                displayName = displayName.trim();

                TextView nameView = createDataTextView(displayName, 20, Typeface.NORMAL);
                nameView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
                nameCellLayout.addView(starIcon);
                nameCellLayout.addView(nameView);

                TextView countView = createDataTextView(String.valueOf(entry.handwashCount), 20, Typeface.BOLD);
                countView.setGravity(Gravity.CENTER);

                dataRow.addView(rankView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f));
                dataRow.addView(nameCellLayout, new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1.5f));
                dataRow.addView(countView, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                table_top_handwashers.addView(dataRow);
                rank++;
            }
        }
    }

    private TextView createTableHeaderTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            textView.setTextColor(ContextCompat.getColor(this, R.color.purple_200));
        } else {
            textView.setTextColor(ContextCompat.getColor(this, R.color.purple_500));
        }
        textView.setTextSize(16);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        return textView;
    }

    private TextView createDataTextView(String text, int textSize, int textStyle) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTypeface(null, textStyle);
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        return textView;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
