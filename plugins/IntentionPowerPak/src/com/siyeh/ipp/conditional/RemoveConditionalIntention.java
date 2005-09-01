/*
 * Copyright 2003-2005 Dave Griffith
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ipp.conditional;

import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ipp.base.Intention;
import com.siyeh.ipp.base.PsiElementPredicate;
import com.siyeh.ipp.psiutils.BoolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

public class RemoveConditionalIntention extends Intention{
    public String getText(){
        return "Simplify ?:";
    }

    public String getFamilyName(){
        return "Remove Pointless Conditional";
    }

    @NotNull
    public PsiElementPredicate getElementPredicate(){
        return new RemoveConditionalPredicate();
    }

    public void processIntention(PsiElement element)
            throws IncorrectOperationException{
        final PsiConditionalExpression exp = (PsiConditionalExpression) element;
        final PsiExpression condition = exp.getCondition();
        final PsiExpression thenExpression = exp.getThenExpression();
        assert thenExpression != null;
        @NonNls final String thenExpressionText = thenExpression.getText();
        if("true".equals(thenExpressionText)){
            final String newExpression = condition.getText();
            replaceExpression(newExpression, exp);
        } else{
            final String newExpression =
                    BoolUtils.getNegatedExpressionText(condition);
            replaceExpression(newExpression, exp);
        }
    }
}
