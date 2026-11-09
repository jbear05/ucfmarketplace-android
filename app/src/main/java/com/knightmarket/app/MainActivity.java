package com.knightmarket.app;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /** Views that return to the previous screen. */
    static final int[] BACK_IDS = {R.id.back, R.id.btnBack, R.id.btnClose};

    /** layout -> {view id, layout it opens}. Screens with no entry only have Back. */
    static final Map<Integer, int[][]> ROUTES = new HashMap<>();

    static {
        int[][] bottomNav = {
                {R.id.nav_home, R.layout.activity_shopping},
                {R.id.nav_saved, R.layout.activity_saved_items},
                {R.id.nav_sell, R.layout.activity_create_listing},
                {R.id.nav_chats, R.layout.activity_messages},
                {R.id.nav_you, R.layout.activity_user_profile},
        };
        // 01 Landing (tap the logo for the screen picker)
        ROUTES.put(R.layout.activity_landing, new int[][]{
                {R.id.btnCreateAccount, R.layout.activity_create_account},
                {R.id.btnLogin, R.layout.activity_login},
                {R.id.logoCard, R.layout.fragment_screen_picker},
        });
        // 02A Create account -> 02B Verify email -> 03 Profile photo -> 07 Home
        ROUTES.put(R.layout.activity_create_account, new int[][]{{R.id.btnContinue, R.layout.activity_verify_email}});
        ROUTES.put(R.layout.activity_verify_email, new int[][]{{R.id.btnVerify, R.layout.activity_profile_photo}});
        ROUTES.put(R.layout.activity_profile_photo, new int[][]{
                {R.id.choose, R.layout.activity_shopping},
                {R.id.skip, R.layout.activity_shopping},
        });
        // 04 Log in
        ROUTES.put(R.layout.activity_login, new int[][]{
                {R.id.btnLogin, R.layout.activity_shopping},
                {R.id.textForgot, R.layout.activity_forgot_password},
                {R.id.btnCreateAccount, R.layout.activity_create_account},
        });
        // 07 Home
        ROUTES.put(R.layout.activity_shopping, concat(bottomNav, new int[][]{
                {R.id.searchContainer, R.layout.activity_search_results},
                {R.id.btnFilters, R.layout.activity_filters},
                {R.id.item1, R.layout.activity_listing_detail},
                {R.id.item2, R.layout.activity_listing_detail},
                {R.id.item3, R.layout.activity_listing_detail},
                {R.id.item4, R.layout.activity_listing_detail},
        }));
        // 08 Search results, 09 Filters
        ROUTES.put(R.layout.activity_search_results, new int[][]{
                {R.id.btnFilters, R.layout.activity_filters},
                {R.id.img1, R.layout.activity_listing_detail},
                {R.id.img2, R.layout.activity_listing_detail},
                {R.id.img3, R.layout.activity_listing_detail},
                {R.id.img4, R.layout.activity_listing_detail},
        });
        ROUTES.put(R.layout.activity_filters, new int[][]{{R.id.btnShowResults, R.layout.activity_search_results}});
        // 11 Listing detail
        ROUTES.put(R.layout.activity_listing_detail, new int[][]{
                {R.id.directions, R.layout.activity_map},
                {R.id.seller, R.layout.activity_seller_profile},
                {R.id.contact, R.layout.activity_chat_thread},
        });
        // 15 Messages -> 16 Chat thread -> 17 Arrange meetup
        ROUTES.put(R.layout.activity_messages, concat(bottomNav, new int[][]{{R.id.conversation, R.layout.activity_chat_thread}}));
        ROUTES.put(R.layout.activity_chat_thread, new int[][]{{R.id.chat_menu, R.layout.activity_arrange_meetup}});
        // 13-14 New listing
        ROUTES.put(R.layout.activity_create_listing, new int[][]{{R.id.publish, R.layout.activity_my_listings}});
        // 19 User profile, 21 My listings
        ROUTES.put(R.layout.activity_user_profile, concat(bottomNav, new int[][]{
                {R.id.profile_listings, R.layout.activity_my_listings},
                {R.id.profile_settings, R.layout.activity_settings},
                {R.id.logout, R.layout.activity_landing},
        }));
        ROUTES.put(R.layout.activity_my_listings, concat(bottomNav, new int[][]{{R.id.new_owned_listing, R.layout.activity_create_listing}}));
        ROUTES.put(R.layout.activity_map, bottomNav);
        ROUTES.put(R.layout.activity_saved_items, bottomNav);
        // Screen picker: every screen in checkpoint order
        ROUTES.put(R.layout.fragment_screen_picker, new int[][]{
                {R.id.open_landing, R.layout.activity_landing},
                {R.id.open_create_account, R.layout.activity_create_account},
                {R.id.open_verify, R.layout.activity_verify_email},
                {R.id.open_photo, R.layout.activity_profile_photo},
                {R.id.open_login, R.layout.activity_login},
                {R.id.open_reset, R.layout.activity_forgot_password},
                {R.id.open_rules, R.layout.activity_community_rules},
                {R.id.open_home, R.layout.activity_shopping},
                {R.id.open_search, R.layout.activity_search_results},
                {R.id.open_filters, R.layout.activity_filters},
                {R.id.open_map, R.layout.activity_map},
                {R.id.open_detail, R.layout.activity_listing_detail},
                {R.id.open_seller, R.layout.activity_seller_profile},
                {R.id.open_create, R.layout.activity_create_listing},
                {R.id.open_messages, R.layout.activity_messages},
                {R.id.open_chat, R.layout.activity_chat_thread},
                {R.id.open_meetup, R.layout.activity_arrange_meetup},
                {R.id.open_saved, R.layout.activity_saved_items},
                {R.id.open_user_profile, R.layout.activity_user_profile},
                {R.id.open_settings, R.layout.activity_settings},
                {R.id.open_my_listings, R.layout.activity_my_listings},
        });
    }

    private static int[][] concat(int[][] a, int[][] b) {
        int[][] out = new int[a.length + b.length][];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ScreenFragment.of(R.layout.activity_landing))
                    .commit();
        }
    }

    /** Opens a screen. Home and Landing clear the back stack so Back doesn't replay the sign-up flow. */
    void show(int layoutRes) {
        FragmentManager fm = getSupportFragmentManager();
        boolean root = layoutRes == R.layout.activity_shopping || layoutRes == R.layout.activity_landing;
        if (root) fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTransaction tx =fm.beginTransaction().replace(R.id.container, ScreenFragment.of(layoutRes));
        if (!root) tx.addToBackStack(null);
        tx.commit();
    }
}
