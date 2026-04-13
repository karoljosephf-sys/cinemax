package com.example.tmbdmadproject;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Container extends AppCompatActivity {
    Fragment currentFragment = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Load HomeFragment by default only if activity is created fresh
        if (savedInstanceState == null) {
            safeLoadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;

            if (id == R.id.nav_home)    fragment = new HomeFragment();
            else if (id == R.id.nav_search)  fragment = new SearchFragment();
            else if (id == R.id.nav_history) fragment = new HistoryFragment();
            else if (id == R.id.nav_stats)   fragment = new StatsFragment();
            else if (id == R.id.nav_profile) fragment = new ProfileFragment();

            if (fragment != null) {
                // ← skip if same fragment type is already showing
                if (currentFragment != null &&
                        currentFragment.getClass() == fragment.getClass()) {
                    return true;
                }
                currentFragment = fragment;
                safeLoadFragment(fragment);
            }

            return true;
        });
    }

    // Safe fragment loader
    private void safeLoadFragment(Fragment fragment) {
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragmentContainer, fragment);
            transaction.commitAllowingStateLoss(); // avoids state loss crashes
        } catch (Exception e) {
            e.printStackTrace(); // logs errors instead of crashing
        }
    }
}