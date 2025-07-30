package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.apache.commons.io.FileUtils;
import org.scijava.prefs.PrefService;

import fiji.plugin.trackmate.LoadTrackMatePlugIn;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TrackMateGeffWriter;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.cli.CLIUtils;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder.ConfigPanel;
import ij.ImageJ;

public class InTRACKtiveLauncher
{
	private static final InTRACKtiveCLI cli = new InTRACKtiveCLI();

	public static final ImageIcon ICON;
	static
	{
		ImageIcon icon = null;
		try
		{
			final URL resource = GuiUtils.getResource( "images/CZ-Biohub-SF-RGB-60x60.png", InTRACKtiveLauncher.class );
			icon = new ImageIcon( resource );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			System.err.println( "Could not load InTRACKtive icon. Using default icon." );
			icon = Icons.TRACK_ICON_64x64;
		}
		ICON = icon;
	}

	private static final String TITLE = "TrackMate InTRACKtive launcher";

	public static final String DOC_URL = "https://github.com/royerlab/inTRACKtive/blob/main/README.md";

	private static final String CONDA_ENV_PREF_KEY = "LastCondaEnv";

	public static void showDialog( final Frame parent, final Model model, final Logger logger, final boolean is2d )
	{
		final PrefService prefService = TMUtils.getContext().getService( PrefService.class );
		final String lastCondaEnv = prefService.get( InTRACKtiveLauncher.class, CONDA_ENV_PREF_KEY, "base" );
		cli.getCommandArg().set( lastCondaEnv );

		final JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );

		/*
		 * HEADER
		 */

		final JPanel header = new JPanel();
		header.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		header.setLayout( new BoxLayout( header, BoxLayout.Y_AXIS ) );

		final JLabel lbl = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lbl.setFont( BIG_FONT );
		lbl.setHorizontalAlignment( SwingConstants.CENTER );
		lbl.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		header.add( lbl );
		final JEditorPane infoDisplay = GuiUtils.infoDisplay( "<html>" + "Documentation for this module "
				+ "<a href=\"" + DOC_URL + "\">on the InTRACKtive repo</a>."
				+ "</html>", false );
		infoDisplay.setMaximumSize( new Dimension( 100_000, 40 ) );
		header.add( Box.createVerticalStrut( 5 ) );
		header.add( infoDisplay );
		final JLabel l2 = new JLabel( "Specify in what conda env is InTRACKtive installed:" );
		l2.setFont( FONT );
		l2.setHorizontalAlignment( SwingConstants.CENTER );
		l2.setAlignmentX( JLabel.CENTER_ALIGNMENT );

		header.add( Box.createVerticalStrut( 15 ) );
		header.add( l2 );

		panel.add( header, BorderLayout.NORTH );
		final ConfigPanel cliPanel = ConfigGuiBuilder.build( cli );
		panel.add( cliPanel, BorderLayout.CENTER );

		// Create a JDialog
		final JPanel dialogPanel = new JPanel();
		dialogPanel.setLayout( new BorderLayout() );
		dialogPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		dialogPanel.add( panel, BorderLayout.CENTER );

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.X_AXIS ) );
		final JButton okButton = new JButton( "OK" );
		final JButton cancelButton = new JButton( "Cancel" );
		buttonPanel.add( Box.createHorizontalGlue() );
		buttonPanel.add( okButton );
		buttonPanel.add( cancelButton );
		dialogPanel.add( buttonPanel, BorderLayout.SOUTH );

		final JDialog dialog = new JDialog( parent, TITLE, false );
		dialog.setContentPane( dialogPanel );
		dialog.pack();
		dialog.setResizable( false );

		// The thread that will run the CLI.
		final Thread thread = new Thread( "TrackMate-InTRACKtive launcher" )
		{

			private Process process = null;

			@Override
			public void run()
			{
				try
				{
					this.process = InTRACKtiveLauncher.run( cli, model, logger, is2d );
					if ( this.process == null )
					{
						logger.error( "Could not launch inTRACKtive." );
						return;
					}

					final int exitValue = process.waitFor();
					final boolean success = exitValue == 0;
					if ( success )
					{
						final String condaEnv = cli.getCommandArg().getValue();
						prefService.put( InTRACKtiveLauncher.class, CONDA_ENV_PREF_KEY, condaEnv );
					}
				}
				catch ( final InterruptedException e )
				{
					logger.log( "Closing inTRACKtive server.\n" );
					process.destroy();
					logger.log( "Done.\n" );
				}
			}
		};

		dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
		dialog.addWindowListener( new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosed( final java.awt.event.WindowEvent e )
			{
				thread.interrupt();
			}
		} );

		cancelButton.addActionListener( e -> dialog.dispose() );
		okButton.addActionListener( e -> {
			panel.remove( cliPanel );
			header.remove( l2 );
			header.revalidate();
			final JLabel infoClose = new JLabel(
					"<html>"
							+ "Launching inTRACKtive server. "
							+ "<p>"
							+ "Closing this window or pressing the button below will kill the server."
							+ "</html>" );
			infoClose.setFont( FONT );
			panel.add( infoClose, BorderLayout.CENTER );
			panel.revalidate();

			okButton.setVisible( false );
			cancelButton.setVisible( false );
			buttonPanel.removeAll();
			thread.start();
			final JButton killButton = new JButton( "Kill inTRACKtive server" );
			killButton.setFont( FONT );
			killButton.addActionListener( e2 -> dialog.dispose() );
			buttonPanel.add( Box.createHorizontalGlue() );
			buttonPanel.add( killButton );
			buttonPanel.revalidate();
		} );
		GuiUtils.positionWindow( dialog, parent );
		dialog.setVisible( true );
	}

	public static Process run( final InTRACKtiveCLI cli, final Model model, final Logger logger, final boolean is2d )
	{
		try
		{
			// Export to a tmp GEFF file.
			final Path maskTmpFolder = Files.createTempDirectory( "TrackMate-InTRACKtive_" );
			CLIUtils.recursiveDeleteOnShutdownHook( maskTmpFolder );
			final String exportPath = maskTmpFolder.resolve( "trackmate.zarr" ).toString();
			TrackMateGeffWriter.export( model, exportPath, is2d );

			// Pass the path to the TrackMat GEFF in the zarr to the CLI.
			final Path geffPath = Paths.get( exportPath ).resolve( TrackMateGeffWriter.GEFF_PREFIX );

			/*
			 * Temporary fixes.
			 *
			 * 1. Remove the 'polygons' subfolder, as it is not in the current
			 * GEFF spec and would cause the validation to fail.
			 */
			final Path polygonsFolder = geffPath.resolve( "nodes/props/polygon" );
			if ( Files.exists( polygonsFolder ) )
				FileUtils.deleteDirectory( polygonsFolder.toFile() );

			/*
			 * 2. Remove the 'covariance2d' and 'covariance3d' subfolders, as
			 * they are not scalar and will mess with the inTRACKtive
			 * requirements for extra data to be scalar.
			 */
			final Path covariance2dFolder = geffPath.resolve( "nodes/props/covariance2d" );
			if ( Files.exists( covariance2dFolder ) )
				FileUtils.deleteDirectory( covariance2dFolder.toFile() );
			final Path covariance3dFolder = geffPath.resolve( "nodes/props/covariance3d" );
			if ( Files.exists( covariance3dFolder ) )
				FileUtils.deleteDirectory( covariance3dFolder.toFile() );

			// Give the path to the GEFF file to the CLI.
			cli.geffFile().set( geffPath.toString() );

			// Run the CLI.
			final String logFile = maskTmpFolder.resolve( "trackmate-intracktive.log" ).toString();
			return CLIUtils.createAndHandleProcess( cli, logger, new File( logFile ) );
		}
		catch ( final IOException e )
		{
			logger.error( "Could not create temp folder to export TrackMate to GEFF file:\n" + e.getMessage() );
		}
		return null;
	}

	public static void main( final String[] args )
	{
		GuiUtils.setSystemLookAndFeel();
		ImageJ.main( args );
		final LoadTrackMatePlugIn plugIn = new LoadTrackMatePlugIn();
		plugIn.run( null );
	}

	public static void main2( final String[] args )
	{
		final String filename = "../TrackMate/samples/MAX_Merged.xml";
		final TmXmlReader reader = new TmXmlReader( new File( filename ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( "Error reading TrackMate XML file: " + reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final Logger logger = Logger.DEFAULT_LOGGER;

		showDialog( null, model, logger, true );
	}
}
