package de.muenchen.mailmerge.dialog.mailmerge.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import de.muenchen.mailmerge.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#radio}.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class RadioButtonUIElement extends UIElement implements
    HasRadioElement
{
  private JRadioButton radio;

  private UIElementAction action;

  private String actionValue;

  private MailMergeParams mmp;

  public RadioButtonUIElement(String label, UIElementAction action,
      final String value, String group, final MailMergeParams mmp)
  {
    super(Box.createHorizontalBox(), group);

    // Die hbox benötige ich, da sonst in Kombination mit einem FromToUIElement ein
    // falsches Alignment verwendet wird.
    Box hbox = (Box) getCompo();
    radio = new JRadioButton(label);
    hbox.add(radio);
    hbox.add(Box.createHorizontalGlue());
    ActionListener li = action.createActionListener(value, mmp);
    if (li != null)
      radio.addActionListener(li);

    this.action = action;
    this.actionValue = value;
    this.mmp = mmp;
  }

  @Override
  public void setButtonGroup(ButtonGroup g)
  {
    g.add(radio);
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    radio.setEnabled(enabled);
  }

  @Override
  public void updateView(Set<String> visibleGroups)
  {
    super.updateView(visibleGroups);
    if (action == UIElementAction.setActionType
      || action == UIElementAction.setOutput)
    {
      ArrayList<String> reasons = new ArrayList<>();
      boolean available =
        mmp.isActionAvailableInCurrentContext(action, actionValue, reasons);
      setEnabled(available);
      // Tooltip zur Anzeige der Probleme zusammen bauen
      if (reasons.isEmpty())
        radio.setToolTipText(null);
      else
      {
        String str = "<html>";
        for (String reason : reasons)
          str += "- " + reason + "<br/>";
        str += "</html>";
        radio.setToolTipText(str);
      }
    }
  }

  @Override
  public boolean isSelected()
  {
    return radio.isSelected();
  }

  @Override
  public void setSelected(boolean b)
  {
    radio.setSelected(b);
    ActionEvent e = new ActionEvent(this, 0, "setSelected");
    for (ActionListener l : radio.getActionListeners())
      l.actionPerformed(e);
  }
}