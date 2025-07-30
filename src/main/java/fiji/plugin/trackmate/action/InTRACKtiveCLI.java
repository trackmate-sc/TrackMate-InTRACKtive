package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator;

public class InTRACKtiveCLI extends CondaCLIConfigurator
{

	private final PathArgument geffFile;

	public InTRACKtiveCLI()
	{
		this.geffFile = addPathArgument()
				.name( "GEFF file" )
				.help( "Path to the GEFF file where TrackMate data is exported." )
				.argument( "" )
				.visible( false )
				.required( true )
				.get();

		addFlag()
				.name( "Export all features" )
				.help( "Add all features to inTRACKtive." )
				.argument( "--add_all_attributes" )
				.visible( false )
				.required( false )
				.inCLI( true )
				.defaultValue( true )
				.get()
				.set();
	}

	public PathArgument geffFile()
	{
		return geffFile;
	}

	@Override
	protected String getCommand()
	{
		return "intracktive open";
	}

}
