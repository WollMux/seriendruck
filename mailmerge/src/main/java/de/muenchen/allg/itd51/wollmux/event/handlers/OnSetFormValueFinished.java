package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;

public class OnSetFormValueFinished extends BasicEvent
{
  private ActionListener listener;

  public OnSetFormValueFinished(ActionListener unlockActionListener)
  {
    this.listener = unlockActionListener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (listener != null) listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}