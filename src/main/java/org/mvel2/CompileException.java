/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2;

import static org.mvel2.util.ParseTools.isWhitespace;
import static org.mvel2.util.ParseTools.repeatChar;

import org.mvel2.util.StringAppender;

import static java.lang.String.copyValueOf;
import static org.mvel2.util.ParseTools.trimLeft;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard exception thrown for all general compile and some runtime failures.
 */
public class CompileException extends RuntimeException {
    private char[] expr;

    private int cursor = 0;
    private int msgOffset = 0;

    private int lineNumber = 1;
    private int column = 0;

    private int lastLineStart = 0;

    private List<ErrorDetail> errors;

    public CompileException(String message, List<ErrorDetail> errors, char[] expr, int cursor, ParserContext ctx) {
        super(message);
        this.expr = expr;
        this.cursor = cursor;

        if (!errors.isEmpty()) {
            ErrorDetail detail = errors.iterator().next();
            this.column = detail.getCol();
            this.lineNumber = detail.getRow();
        }

        this.errors = errors;
    }

    public String toString() {
        return generateErrorMessage();
    }

    public CompileException(String message, char[] expr, int cursor, Throwable e) {
        super(message, e);
        this.expr = expr;
        this.cursor = cursor;
    }

    public CompileException(String message, char[] expr, int cursor) {
        super(message);
        this.expr = expr;
        this.cursor = cursor;
    }


    @Override
    public String getMessage() {
        return generateErrorMessage();
    }

    private void calcRowAndColumn() {
        int row = 1;
        int col = 1;

        if ((lineNumber != 0 && column != 0) || expr == null || expr.length == 0) return;

        for (int i = 0; i < cursor; i++) {
            switch (expr[i]) {
                case '\r':
                    continue;
                case '\n':
                    row++;
                    col = 0;
                    break;

                default:
                    col++;
            }
        }

        this.lineNumber = row;
        this.column = col;


    }

    private CharSequence showCodeNearError(char[] expr, int cursor) {
        if (expr == null) return "Unknown";

        int start = cursor - 20;
        int end = (cursor + 30);

        if (end > expr.length) {
            end = expr.length;
            start -= 30;
        }

        if (start < 0) {
            start = 0;
        }

        String cs;

        int firstCr;
        int lastCr;

        try {
            cs = copyValueOf(expr, start, end - start);
        }
        catch (StringIndexOutOfBoundsException e) {
            System.out.println("");
            throw e;
        }

        String match = new String(expr, cursor, expr.length - cursor);
        Makematch: for (int i = 0; i < match.length(); i++) {
            switch (match.charAt(i)) {
                case '\n':
                    match = match.substring(0, i);
                    break Makematch;
                default:
                    if (isWhitespace(match.charAt(i))) {
                        match = match.substring(0, i);
                        break Makematch;
                    }
            }
        }

        if (match.length() >= 30) {
            match = match.substring(0, 30);
        }


//        int renderColumnOffset = 0;

        do {
            firstCr = cs.indexOf('\n');
            lastCr = cs.lastIndexOf('\n');

            if (firstCr == -1) break;

            int matchIndex = cs.indexOf(match);

            if (firstCr != -1 && firstCr == lastCr) {
                if (firstCr > matchIndex) {
                    cs = cs.substring(0, firstCr);
                }
                else if (firstCr < matchIndex) {
                    cs = cs.substring(firstCr + 1, cs.length() - (firstCr + 1));
          //          renderColumnOffset += firstCr;
                }
            }
            else {
                cs = cs.substring(firstCr + 1, lastCr);
        //        renderColumnOffset += firstCr;
            }
        }
        while (true);

//        for (int i = 0; i < cs.length(); i++) {
//            if (isWhitespace(cs.charAt(i))) renderColumnOffset++;
//            else break;
//        }

//        msgOffset = start + renderColumnOffset;

        String trimmed = cs.trim();

        msgOffset = trimmed.indexOf(match);

        return trimmed;
    }

    private String generateErrorMessage() {
        StringAppender appender = new StringAppender().append("[Error: " + super.getMessage() + "]\n");

        int offset = appender.length();

        appender.append("[Near : {... ");

        offset = appender.length() - offset;

        appender.append(showCodeNearError(expr, cursor))
                .append(" ....}]\n")
                .append(repeatChar(' ', offset));

  //      if ((offset = cursor - msgOffset - 2) < 0) offset = 0;

  //      appender.append(repeatChar(' ', offset + (offset > 0 ? 1 : 0))).append("^");

        if (msgOffset < 0) msgOffset = 0;

        appender.append(repeatChar(' ', msgOffset)).append('^');

        calcRowAndColumn();

        if (lineNumber != -1) {
            appender.append('\n')
                    .append("[Line: " + lineNumber + ", Column: " + (column+1) + "]");
        }
        return appender.toString();
    }

    public char[] getExpr() {
        return expr;
    }

    public int getCursor() {
        return cursor;
    }

    public List<ErrorDetail> getErrors() {
        return errors != null ? errors : new ArrayList<ErrorDetail>(0);
    }

    public void setErrors(List<ErrorDetail> errors) {
        this.errors = errors;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setExpr(char[] expr) {
        this.expr = expr;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public int getLastLineStart() {
        return lastLineStart;
    }

    public void setLastLineStart(int lastLineStart) {
        this.lastLineStart = lastLineStart;
    }
}
