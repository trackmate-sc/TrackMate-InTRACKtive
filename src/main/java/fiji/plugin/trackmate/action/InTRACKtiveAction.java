/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.action;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.GeffIOUtils;

public class InTRACKtiveAction extends AbstractTMAction
{

	public static final String NAME = "Open in InTRACKtive viewer";

	public static final String KEY = "INTRACKTIVE_VIEWER";

	public static final String INFO_TEXT = "<html>"
			+ "Visualize the current tracking data in the InTRACKtive viewer."
			+ "<p>"
			+ "The InTRACKtive viewer is a web-based application for visualizing and analyzing tracking data."
			+ "It must be installed in a conda environment."
			+ "See the <a url=\"" + InTRACKtiveLauncher.DOC_URL + "\">InTRACKtive webpage</a> for details:"
			+ InTRACKtiveLauncher.DOC_URL
			+ "</html>";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		final boolean is2d = GeffIOUtils.is2D( trackmate );
		InTRACKtiveLauncher.showDialog( parent, trackmate.getModel(), logger, is2d );
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new InTRACKtiveAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return InTRACKtiveLauncher.ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getUrl()
		{
			return InTRACKtiveLauncher.DOC_URL;
		}
	}
}
