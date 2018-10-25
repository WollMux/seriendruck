package de.muenchen.mailmerge.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.frame.XFrame;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.event.DispatchProviderAndInterceptor;

/**
 * Erzeugt ein neues WollMuxEvent zum Registrieren eines (frischen)
 * {@link DispatchProviderAndInterceptor} auf frame.
 */
public class OnRegisterDispatchInterceptor extends BasicEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OnRegisterDispatchInterceptor.class);

  private XFrame frame;

  public OnRegisterDispatchInterceptor(XFrame frame)
  {
    this.frame = frame;
  }

  @Override
  protected void doit()
  {
    if (frame == null)
    {
      LOGGER.debug(L.m("Ignoriere handleRegisterDispatchInterceptor(null)"));
      return;
    }
    try
    {
      DispatchProviderAndInterceptor.registerDocumentDispatchInterceptor(frame);
    }
    catch (java.lang.Exception e)
    {
      LOGGER.error(L.m("Kann DispatchInterceptor nicht registrieren:"), e);
    }

    // Sicherstellen, dass die Schaltflächen der Symbolleisten aktiviert werden:
    try
    {
      frame.contextChanged();
    }
    catch (java.lang.Exception e)
    {}
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + ((frame != null) ? frame.hashCode() : "Kein Frame") + ")";
  }
}