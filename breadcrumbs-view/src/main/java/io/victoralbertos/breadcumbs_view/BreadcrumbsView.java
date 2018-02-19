/*
 * Copyright 2016 Victor Albertos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.victoralbertos.breadcumbs_view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BreadcrumbsView extends RelativeLayout {

    int dotId = 101;
    int separatorId = 201;
    int visitedStepBorderDotColor;
    int visitedStepFillDotColor;
    int nextStepBorderDotColor;
    int nextStepFillDotColor;
    int visitedStepSeparatorColor;
    int nextStepSeparatorColor;
    int radius;
    int sizeDotBorder;
    int heightSeparator;
    int nSteps;
    int currentStep = 0;
    int textSize;
    int textColorVisited;
    int textColorNext;
    int textTopMargin;
    List<Step> steps;
    List<String> texts;
    boolean animIsRunning;
    boolean isSeparatorOnStart;

    public BreadcrumbsView(Context context, int nSteps) {
        super(context);
        this.nSteps = nSteps;
        texts = new ArrayList<>();
        PropertiesHelper.init(this);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                createSteps();
            }
        });
    }

    public BreadcrumbsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        texts = new ArrayList<>();
        PropertiesHelper.init(this, attrs);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                createSteps();
            }
        });
    }

    /**
     * Start counting from 0.
     *
     * @return the index of the current step.
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Move to the next step. Throw if not steps are left to move forward
     *
     * @throws IndexOutOfBoundsException
     */
    public void nextStep() throws IndexOutOfBoundsException {
        if (animIsRunning || currentStep == nSteps - 1) return;
        animIsRunning = true;
        SeparatorView separatorView = steps.get(currentStep).separatorView;
        final DotView dotView = steps.get(currentStep + 1).dotView;
        if (currentStep < texts.size() - 1) {
            steps.get(currentStep + 1).textView.setTextColor(textColorVisited);
        }
        separatorView.animateFromNextStepToVisitedStep(new Runnable() {
            @Override
            public void run() {
                dotView.animateFromNextStepToVisitedStep(new Runnable() {
                    @Override
                    public void run() {
                        currentStep++;
                        animIsRunning = false;
                    }
                });
            }
        });
    }

    /**
     * Move to the previous step. Throw if not steps are left to go back.
     *
     * @throws IndexOutOfBoundsException
     */
    public void prevStep() throws IndexOutOfBoundsException {
        if (animIsRunning || currentStep == 0) return;
        animIsRunning = true;
        DotView dotView = steps.get(currentStep).dotView;
        final SeparatorView separatorView = steps.get(currentStep - 1).separatorView;
        steps.get(currentStep).textView.setTextColor(textColorNext);
        dotView.animateFromVisitedStepToNextStep(new Runnable() {
            @Override
            public void run() {
                separatorView.animateFromVisitedStepToNextStep(new Runnable() {
                    @Override
                    public void run() {
                        currentStep--;
                        animIsRunning = false;
                    }
                });
            }
        });
    }

    /**
     * Should be called before this view is measured. Otherwise throw an IllegalStateException.
     *
     * @param currentStep the desired step
     */
    public void setCurrentStep(int currentStep) throws IllegalStateException {
        if (steps != null) {
            throw new IllegalStateException(
                    "Illegal attempt to set the value of the current step once the view has been measured");
        }
        this.currentStep = currentStep;
    }

    private void createSteps() {
        if (nSteps < 2) throw new IllegalArgumentException("Number of steps must be greater than 1");
        nSteps = isSeparatorOnStart ? nSteps + 1 : nSteps;
        int lastElement = isSeparatorOnStart ? nSteps - 2 : nSteps - 1;
        int nSeparators = nSteps;
        int widthDot = radius * 2;
        int widthStep = ((getWidth() - widthDot) / (nSeparators - 1)) - widthDot;
        steps = new ArrayList<>();
        // Initial separator
        if (isSeparatorOnStart) {
            SeparatorView firstSeparator = createSeparator(widthStep, true);
            addView(firstSeparator);
            steps.add(new Step(firstSeparator, null, null));
        }
        for (int position = 0; position < nSteps; position++) {
            boolean visited = position <= currentStep || position == 0;
            final DotView dotView = createDotView(visited);
            TextView textView = null;
            addView(dotView);
            if (position < texts.size()) {
                textView = createText(position);
                addView(textView);
            }
            //Prevent drawing a separator after the last dot.
            if (position == lastElement) {
                steps.add(new Step(null, dotView, textView));
                break;
            }
            SeparatorView separatorView = createSeparator(widthStep, position < currentStep);
            addView(separatorView);
            steps.add(new Step(separatorView, dotView, textView));
        }
        locateOnScreen();
    }

    private DotView createDotView(boolean isVisited) {
        DotView dotView = new DotView(getContext(), isVisited, visitedStepBorderDotColor,
                visitedStepFillDotColor,
                nextStepBorderDotColor, nextStepFillDotColor, radius,
                sizeDotBorder);
        dotView.setId(++dotId);
        return dotView;
    }

    private TextView createText(int position) {
        TextView textView = new TextView(getContext());
        textView.setText(texts.get(position));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textView.setAllCaps(true);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(position == 0 ? textColorVisited : textColorNext);
        return textView;
    }

    private SeparatorView createSeparator(int widthStep, boolean isVisited) {
        SeparatorView separatorView = new SeparatorView(getContext(), isVisited, visitedStepSeparatorColor,
                nextStepSeparatorColor, widthStep,
                heightSeparator);
        separatorView.setId(++separatorId);
        return separatorView;
    }

    private ViewTreeObserver.OnGlobalLayoutListener getLayoutListener(final View dotView, final View textView) {
        return new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int dotRadius = dotView.getWidth();
                int textWidth = textView.getWidth();
                int margin = (textWidth - dotRadius) / 2;
                textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                RelativeLayout.LayoutParams textParams = (RelativeLayout.LayoutParams) textView.getLayoutParams();
                textParams.setMargins(-margin, textTopMargin, 0, 0);
            }
        };
    }

    private void locateOnScreen() {
        for (int i = 0; i < steps.size(); i++) {
            Step currentStep = steps.get(i);
            if (i == 0 && isSeparatorOnStart) {
                RelativeLayout.LayoutParams separatorParams = (RelativeLayout.LayoutParams) currentStep.separatorView.getLayoutParams();
                separatorParams.topMargin = radius;
                continue;
            } else if (i == 0) {
                RelativeLayout.LayoutParams textParams = (RelativeLayout.LayoutParams) currentStep.textView.getLayoutParams();
                RelativeLayout.LayoutParams separatorParams = (RelativeLayout.LayoutParams) currentStep.separatorView.getLayoutParams();
                textParams.addRule(BELOW, currentStep.dotView.getId());
                textParams.addRule(ALIGN_LEFT, currentStep.dotView.getId());
                separatorParams.addRule(RIGHT_OF, currentStep.dotView.getId());
                separatorParams.topMargin = radius;
                currentStep.textView.getViewTreeObserver().addOnGlobalLayoutListener(getLayoutListener(
                        currentStep.dotView, currentStep.textView));
                continue;
            }
            Step previousStep = steps.get(i - 1);
            RelativeLayout.LayoutParams dotParams = (RelativeLayout.LayoutParams) currentStep.dotView.getLayoutParams();
            dotParams.addRule(RIGHT_OF, previousStep.separatorView.getId());
            if (currentStep.textView != null) {
                RelativeLayout.LayoutParams textParams = (RelativeLayout.LayoutParams) currentStep.textView.getLayoutParams();
                textParams.addRule(BELOW, currentStep.dotView.getId());
                textParams.addRule(ALIGN_LEFT, currentStep.dotView.getId());
                currentStep.textView.getViewTreeObserver().addOnGlobalLayoutListener(getLayoutListener(
                        currentStep.dotView, currentStep.textView));
            }
            if (currentStep.separatorView != null) {
                RelativeLayout.LayoutParams separatorParams = (RelativeLayout.LayoutParams) currentStep.separatorView.getLayoutParams();
                separatorParams.addRule(RIGHT_OF, currentStep.dotView.getId());
                separatorParams.topMargin = radius;
            }
        }
    }

    private static class Step {
        private final SeparatorView separatorView;
        private final DotView dotView;
        private final TextView textView;

        public Step(SeparatorView separatorView, DotView dotView, TextView textView) {
            this.separatorView = separatorView;
            this.dotView = dotView;
            this.textView = textView;
        }
    }
}
