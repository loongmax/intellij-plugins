package org.jetbrains.training.sandbox;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.awt.RelativePoint;
import org.jdom.Element;
import org.jetbrains.training.ActionsRecorder;
import org.jetbrains.training.Lesson;
import org.jetbrains.training.graphics.DetailPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Created by karashevich on 30/01/15.
 */
public class LessonProcessor {

    private static boolean isRecording = false;

    public static void processLesson(final Lesson lesson, final Editor editor, final AnActionEvent e, Document document, String target, final DetailPanel infoPanel) throws InterruptedException {
        if (lesson.getScn().equals(null)) {
            System.err.println("Scenario is empty or cannot be read!");
            return;
        }
        if (lesson.getScn().getRoot().equals(null)) {
            System.err.println("Scenario is empty or cannot be read!");
            return;
        }

        for (final Element element : lesson.getScn().getRoot().getChildren()) {
            if (element.getName().equals("TypeText")) {


                if (element.getAttribute("description") != null) {
                    final String description = (element.getAttribute("description").getValue().toString());
//                                            myPainter.setText(description);
                    infoPanel.setText(description);
                }

                if (element.getAttribute("btn") != null) {
                    final String buttonText =(element.getAttribute("btn").getValue().toString());
                    infoPanel.setButtonText(buttonText);
                    infoPanel.addWaitToButton(editor);
                } else {
                    infoPanel.hideButton();
                }

                final String finalText = (element.getContent().isEmpty() ? "" : element.getContent().get(0).getValue());
                boolean isTyping = true;
                final int[] i = {0};
                final int initialOffset = editor.getCaretModel().getOffset();

                while (isTyping) {
                    Thread.sleep(20);
                    final int finalI = i[0];
                    WriteCommandAction.runWriteCommandAction(e.getProject(), new Runnable() {
                        @Override
                        public void run() {
                            editor.getDocument().insertString(finalI + initialOffset, finalText.subSequence(i[0], i[0] + 1));
                            editor.getCaretModel().moveToOffset(finalI + 1 + initialOffset);
                        }
                    });
                    isTyping = (++i[0] < finalText.length());
                }
            } else if(element.getName().equals("Action")) {

                if (element.getAttribute("description") != null) {
                    final String description = (element.getAttribute("description").getValue().toString());
                    infoPanel.setText(description);
                    //                                            myPainter.setText(description);
                }

                if (element.getAttribute("btn") != null) {
                    final String buttonText =(element.getAttribute("btn").getValue().toString());
                    infoPanel.setButtonText(buttonText);
                    infoPanel.addWaitToButton(editor);
                } else {
                    infoPanel.hideButton();
                }

                final String actionType = (element.getAttribute("action").getValue().toString());

                if (element.getAttribute("balloon") != null) {

                    final String balloonText =(element.getAttribute("balloon").getValue().toString());

                    synchronized (editor) {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    int delay = 0;
                                    if (element.getAttribute("delay") != null) {
                                        delay = Integer.parseInt(element.getAttribute("delay").getValue().toString());
                                    }
                                    showBalloon(e, editor, balloonText, delay);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                            }

                        });
                        editor.wait();
                    }

                }
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        performAction(actionType, editor);
                    }
                });
                //To see result
                Thread.sleep(500);

            } else if(element.getName().equals("Start")) {
                if (element.getAttribute("description") != null) {
                    final String description =(element.getAttribute("description").getValue().toString());
                    infoPanel.setText(description);
                }
                if (element.getAttribute("btn") != null) {
                    final String buttonText =(element.getAttribute("btn").getValue().toString());
                    infoPanel.setButtonText(buttonText);
                    infoPanel.addWaitToButton(editor);
                }
                synchronized (editor) {
                    editor.wait();
                }
            } else if(element.getName().equals("Text")) {
                if (element.getAttribute("description") != null) {
                    final String description =(element.getAttribute("description").getValue().toString());
                    infoPanel.setText(description);
                }
                if (element.getAttribute("btn") != null) {
                    final String buttonText =(element.getAttribute("btn").getValue().toString());
                    infoPanel.setButtonText(buttonText);
                    infoPanel.addWaitToButton(editor);

                    synchronized (editor) {
                        editor.wait();
                    }
                } else {
                    infoPanel.hideButton();
                }

            } else if(element.getName().equals("MoveCaret")) {


                final String offsetString =(element.getAttribute("offset").getValue().toString());
                final int offset = Integer.parseInt(offsetString);


                WriteCommandAction.runWriteCommandAction(e.getProject(), new Runnable() {
                    @Override
                    public void run() {
                        editor.getCaretModel().moveToOffset(offset);
                    }
                });
            } else if(element.getName().equals("CopyText")) {

                if (element.getAttribute("btn") != null) {
                    final String buttonText = (element.getAttribute("btn").getValue().toString());
                    infoPanel.setButtonText(buttonText);
                    infoPanel.addWaitToButton(editor);
                } else {
                    infoPanel.hideButton();
                }

                final String finalText = (element.getContent().isEmpty() ? "" : element.getContent().get(0).getValue());
                WriteCommandAction.runWriteCommandAction(e.getProject(), new Runnable() {
                    @Override
                    public void run() {
                        editor.getDocument().insertString(0, finalText);
                    }
                });

            } else if(element.getName().equals("Try")) {

                if (element.getAttribute("description") != null) {
                    final String description =(element.getAttribute("description").getValue().toString());
                    infoPanel.setText(description);
                }

                if (element.getAttribute("btn") != null) {
                    final String buttonText =(element.getAttribute("btn").getValue().toString());
                    infoPanel.setButtonText(buttonText);
                    infoPanel.addWaitToButton(editor);
                } else {
                    infoPanel.hideButton();
                }

                final ActionsRecorder recorder = new ActionsRecorder(e.getProject(), document, target);
                isRecording = true;
                Disposer.register(recorder, new Disposable() {
                    @Override
                    public void dispose() {
                        isRecording = false;
                    }
                });
                recorder.startRecording(new Runnable() {
                    @Override
                    public void run() {
                        infoPanel.setText("Awesome, now you know how to duplicate lines easily!");
                        infoPanel.greenalize();
                        lesson.pass();
                    }
                });

            } else {

            }
        }
    }

    /**
     * performing internal platform action
     * @param actionName - name of IntelliJ Action. For full list please see http://git.jetbrains.org/?p=idea/community.git;a=blob;f=platform/platform-api/src/com/intellij/openapi/actionSystem/IdeActions.java;hb=HEAD
     * @param lockEditor - using for suspending typing robot until this action will be performed
     */
    private static void performAction(String actionName, final Editor lockEditor){
        final ActionManager am = ActionManager.getInstance();
        final AnAction targetAction = am.getAction(actionName);
        final InputEvent inputEvent = getInputEvent(actionName);

        am.tryToExecute(targetAction, inputEvent, null, null, false).doWhenDone(new Runnable() {
            @Override
            public void run() {
                synchronized (lockEditor){
                    lockEditor.notifyAll();
                }
            }
        });

    }


    /**
     * Some util method for <i>performAction</i> method
     * @param actionName - please see it in <i>performAction</i> method
     * @return
     */
    public static InputEvent getInputEvent(String actionName) {
        final Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionName);
        KeyStroke keyStroke = null;
        for (Shortcut each : shortcuts) {
            if (each instanceof KeyboardShortcut) {
                keyStroke = ((KeyboardShortcut)each).getFirstKeyStroke();
                if (keyStroke != null) break;
            }
        }

        if (keyStroke != null) {
            return new KeyEvent(JOptionPane.getRootFrame(),
                    KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    keyStroke.getModifiers(),
                    keyStroke.getKeyCode(),
                    keyStroke.getKeyChar(),
                    KeyEvent.KEY_LOCATION_STANDARD);
        } else {
            return new MouseEvent(JOptionPane.getRootFrame(), MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 1, false, MouseEvent.BUTTON1);
        }
    }


    /**
     *
     * @param e
     * @param editor - editor where to show balloon, also uses for locking while balloon appearing
     */
    private static void showBalloon(AnActionEvent e, final Editor editor, String balloonText, final int delay) throws InterruptedException {
        FileEditorManager instance = FileEditorManager.getInstance(e.getProject());
        if (instance == null) return;
        if (editor == null) return;

        int offset = editor.getCaretModel().getCurrentCaret().getOffset();
        VisualPosition position = editor.offsetToVisualPosition(offset);
        Point point = editor.visualPositionToXY(position);
        BalloonBuilder builder =
                JBPopupFactory.getInstance().
                        createHtmlTextBalloonBuilder(balloonText, null, Color.LIGHT_GRAY, null)
                        .setHideOnClickOutside(false).setCloseButtonEnabled(true).setHideOnKeyOutside(false);
        final Balloon myBalloon = builder.createBalloon();

        myBalloon.show(new RelativePoint(editor.getContentComponent(), point), Balloon.Position.above);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                    myBalloon.hide();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

            }
        }).start();

        myBalloon.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(LightweightWindowEvent lightweightWindowEvent) {

            }

            @Override
            public void onClosed(LightweightWindowEvent lightweightWindowEvent) {
                synchronized (editor){
                    editor.notifyAll();
                }
            }
        });

    }

}