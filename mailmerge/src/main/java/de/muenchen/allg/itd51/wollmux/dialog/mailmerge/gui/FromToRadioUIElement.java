package de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.dialog.NonNumericKeyConsumer;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.DatasetSelectionType;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#fromtoradio}.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class FromToRadioUIElement extends UIElement implements
    HasRadioElement
{
  private JTextField start;

  private JTextField end;

  private JRadioButton fromRadioButton;

  private MailMergeParams mmp;

  /**
   * Falls {@link DatasetSelectionType} != {@link DatasetSelectionType#ALL}, so
   * bestimmt dies die Indizes der ausgewählten Datensätze.
   */
  private IndexSelection indexSelection = new IndexSelection();

  public FromToRadioUIElement(String labelFrom, String labelTo,
      UIElementAction action, final String value, String group,
      final MailMergeParams mmp)
  {
    super(Box.createHorizontalBox(), group, mmp);
    this.mmp = mmp;

    Box hbox = (Box) getCompo();
    fromRadioButton = new JRadioButton(labelFrom);
    hbox.add(fromRadioButton);
    start = new JTextField(4);
    start.addKeyListener(NonNumericKeyConsumer.instance);
    start.getDocument().addDocumentListener(rangeDocumentListener);
    DimAdjust.fixedSize(start);
    hbox.add(start);
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(new JLabel(labelTo));
    hbox.add(Box.createHorizontalStrut(5));
    end = new JTextField(4);
    end.addKeyListener(NonNumericKeyConsumer.instance);
    end.getDocument().addDocumentListener(rangeDocumentListener);
    DimAdjust.fixedSize(end);
    hbox.add(end);
    hbox.add(Box.createHorizontalGlue());
    ActionListener li = action.createActionListener(value, mmp);
    if (li != null) fromRadioButton.addActionListener(li);
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);
  }

  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    fromRadioButton.setEnabled(enabled);
  }

  public void setButtonGroup(ButtonGroup g)
  {
    g.add(fromRadioButton);
  }

  public boolean isSelected()
  {
    return fromRadioButton.isSelected();
  }

  public void setSelected(boolean b)
  {
    fromRadioButton.setSelected(b);
    ActionEvent e = new ActionEvent(this, 0, "setSelected");
    for (ActionListener l : fromRadioButton.getActionListeners())
      l.actionPerformed(e);
  }

  private DocumentListener rangeDocumentListener = new DocumentListener()
  {
    public void update()
    {
      fromRadioButton.setSelected(true);
      mmp.setDatasetSelectionType(DatasetSelectionType.RANGE);
      try
      {
        indexSelection.setRangeStart(Integer.parseInt(start.getText()));
      }
      catch (Exception x)
      {
        indexSelection.setRangeStart(0);
      }
      try
      {
        indexSelection.setRangeEnd(Integer.parseInt(end.getText()));
      }
      catch (Exception x)
      {
        indexSelection.setRangeEnd(0);
      }
    }

    public void insertUpdate(DocumentEvent e)
    {
      update();
    }

    public void removeUpdate(DocumentEvent e)
    {
      update();
    }

    public void changedUpdate(DocumentEvent e)
    {
      update();
    }
  };

  public void addSubmitArgs(java.util.Map<SubmitArgument, Object> args)
      throws InvalidArgumentException
  {
    args.put(SubmitArgument.indexSelection, indexSelection);
  }
}