/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.commons.swing.ComponentUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 */
public class IndeterminateProgressMonitor
{
    private final Component parent;
    private final String note;
    private final boolean modal;
    private final Runnable onCancel;
    private JDialog dialog;
    private JLabel noteLabel = null;
    private JButton cancelButton = null;
    private JProgressBar progressBar;

    public IndeterminateProgressMonitor(Component parent, String note, boolean modal, Runnable onCancel)
    {
        this.parent = parent;
        this.note = note;
        this.modal = modal;
        this.onCancel = onCancel;

        initComponents();
    }

    public boolean isVisible()
    {
        return dialog.isVisible();
    }

    @InvokeOnEDT
    public void setVisible(final boolean visible)
    {
        dialog.setVisible(visible);
        dialog.dispose();
    }

    public void onCancel()
    {
        if (onCancel != null)
        {
            setCancelView();
            onCancel.run();
        }
    }

    @InvokeOnEDT
    public void enableStringPainted(final boolean enable)
    {
        progressBar.setStringPainted(enable);
    }

    @InvokeOnEDT
    public void setProgressMessage(final String message)
    {
        enableStringPainted(true);
        progressBar.setString(message);
    }

    @InvokeOnEDT
    private void setCancelView()
    {
        if (noteLabel != null) noteLabel.setText("Canceling...");
        if (cancelButton != null) cancelButton.setEnabled(false);
    }

    @InvokeAndWaitOnEDT
    private void initComponents()
    {
        if (parent instanceof Dialog)
        {
            dialog = new JDialog((Dialog) parent, modal);
        } else if (parent instanceof Frame)
        {
            dialog = new JDialog((Frame) parent, modal);
        } else
        {
            dialog = new JDialog((Frame) null, modal);
        }
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent windowEvent)
            {
                onCancel();
            }
        });

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> onCancel());

        dialog.getContentPane().setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        {
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

            if (note != null)
            {
                noteLabel = new JLabel(note);
                noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(noteLabel);
                panel.add(Box.createVerticalStrut(10));
            }

            JPanel progressPanel = new JPanel();
            progressPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            {
                progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.LINE_AXIS));
                progressPanel.add(progressBar);
                progressPanel.add(Box.createHorizontalStrut(5));
                progressPanel.add(cancelButton);
            }
            panel.add(progressPanel);
        }
        dialog.getContentPane().add(panel, BorderLayout.CENTER);
        dialog.validate();
        dialog.pack();

        ComponentUtils.centerComponentOnScreen(dialog);
    }
}
