package de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.DatasetSelectionType;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt die möglichen Actions, die auf Formularelemente des
 * Seriendruckabschnitts angewendet werden können und deren Event-Handler.
 * 
 * Die Methode {@link #getByname(String)} ermöglicht eine Zuordnung von Strings der
 * Konfigurationsdatei auf den entsprechenden enum-Typen.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public enum UIElementAction {
  setActionType() {
    public ActionListener createActionListener(final String value,
        final MailMergeParams mmp)
    {
      return new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          mmp.setCurrentActionType(value);
          mmp.updateView();
        }
      };
    }
  },

  setOutput() {
    public ActionListener createActionListener(final String value,
        final MailMergeParams mmp)
    {
      return new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          mmp.setCurrentOutput(value);
          mmp.updateView();
        }
      };
    }
  },

  selectAll() {
    public ActionListener createActionListener(final String value,
        final MailMergeParams mmp)
    {
      return new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          mmp.setDatasetSelectionType(DatasetSelectionType.ALL);
        }
      };
    }
  },

  selectRange() {
    public ActionListener createActionListener(final String value,
        final MailMergeParams mmp)
    {
      return new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          mmp.setDatasetSelectionType(DatasetSelectionType.RANGE);
        }
      };
    }
  },

  abort() {
    public ActionListener createActionListener(final String value,
        final MailMergeParams mmp)
    {
      return new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          mmp.getDialog().dispose();
        }
      };
    }
  },

  submit() {
    public ActionListener createActionListener(final String value,
        final MailMergeParams mmp)
    {
      return new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          try
          {
            Map<SubmitArgument, Object> args =
              new HashMap<SubmitArgument, Object>();
            for (Section s : mmp.getSections())
              s.addSubmitArgs(args);
            mmp.getDialog().dispose();
            boolean ignoreDocPrintFuncs = false;
            if (mmp.getIgnoreDocPrintFuncs() != null && mmp.getIgnoreDocPrintFuncs() == true)
              ignoreDocPrintFuncs = true;
            mmp.getMMC().doMailMerge(mmp.getUsePrintFunctions(), ignoreDocPrintFuncs,
              mmp.getDatasetSelectionType(), args);
          }
          catch (InvalidArgumentException ex)
          {
            if (ex.getMessage() != null)
              JOptionPane.showMessageDialog(mmp.getDialog(), ex.getMessage(),
                L.m("Fehlerhafte Eingabe"), JOptionPane.ERROR_MESSAGE);
          }
        }
      };
    }
  },

  unknown() {
    public ActionListener createActionListener(final String value,
        final MailMergeParams mmp)
    {
      return null;
    }
  };

  public static UIElementAction getByname(String s)
  {
    for (UIElementAction a : UIElementAction.values())
    {
      if (a.toString().equalsIgnoreCase(s)) return a;
    }
    return unknown;
  }

  public abstract ActionListener createActionListener(String value,
      MailMergeParams mmp);
}