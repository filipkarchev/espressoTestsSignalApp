package org.thoughtcrime.securesms;


import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ConversationListActivityTest2 {

    @Rule
    public ActivityTestRule<ConversationListActivity> mActivityTestRule = new ActivityTestRule<>(ConversationListActivity.class);

    @Test
    public void conversationListActivityTest2() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton = onView(
                allOf(withId(android.R.id.button2), withText("Not now"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                2)));
        appCompatButton.perform(scrollTo(), click());

        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.information_label), withText("More information"),
                        childAtPosition(
                                allOf(withId(R.id.registration_container),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                3)),
                                6)));
        appCompatTextView.perform(scrollTo(), click());

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.country_code), withText("1"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.registration_container),
                                        1),
                                1)));
        appCompatEditText.perform(scrollTo(), replaceText("51"));

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.country_code), withText("51"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.registration_container),
                                        1),
                                1),
                        isDisplayed()));
        appCompatEditText2.perform(closeSoftKeyboard());

        ViewInteraction editText = onView(
                allOf(withId(R.id.country_code), withText("51"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.registration_container),
                                        1),
                                1),
                        isDisplayed()));
        editText.check(matches(withText("52")));
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
