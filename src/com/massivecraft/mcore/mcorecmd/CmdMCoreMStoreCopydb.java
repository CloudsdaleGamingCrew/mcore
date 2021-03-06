package com.massivecraft.mcore.mcorecmd;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import com.massivecraft.mcore.MCore;
import com.massivecraft.mcore.MCorePerm;
import com.massivecraft.mcore.cmd.MCommand;
import com.massivecraft.mcore.cmd.req.ReqHasPerm;
import com.massivecraft.mcore.store.Coll;
import com.massivecraft.mcore.store.Db;
import com.massivecraft.mcore.store.Driver;
import com.massivecraft.mcore.store.MStore;
import com.massivecraft.mcore.xlib.gson.JsonElement;

public class CmdMCoreMStoreCopydb extends MCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdMCoreMStoreCopydb()
	{
		// Aliases
		this.addAliases("copydb");
		
		// Args
		this.addRequiredArg("from");
		this.addRequiredArg("to");
		
		// Requirements
		this.addRequirements(ReqHasPerm.get(MCorePerm.CMD_MCORE_MSTORE_COPYDB.node));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform()
	{
		// Args
		final String fromAlias = this.arg(0);
		final Db fromDb = MStore.getDb(fromAlias);
		if (fromDb == null)
		{
			msg("<b>could not get the from-database.");
			return;
		}
		
		final String toAlias = this.arg(1);
		final Db toDb = MStore.getDb(toAlias);
		if (toDb == null)
		{
			msg("<b>could not get the to-database.");
			return;
		}
		
		// Prepare
		final Driver fromDriver = fromDb.getDriver();
		final Driver toDriver = toDb.getDriver();
		
		Set<String> collnames = fromDb.getCollnames();
		
		// Statistics
		int countCollCurrent = 0;
		int countCollTotal = collnames.size();
		
		// Do it!
		long before = System.currentTimeMillis();
		msg("<i>Now copying database with <h>%d <i>collections.", countCollTotal);
		for (String collname : fromDb.getCollnames())
		{
			countCollCurrent++;
			final Coll<?> fromColl = new Coll<Object>(collname, Object.class, fromDb, MCore.get());
			final Coll<?> toColl = new Coll<Object>(collname, Object.class, toDb, MCore.get());
			
			Collection<String> ids = fromDriver.getIds(fromColl);
			msg("<i>Now copying collection <h>%d/%d %s <i>with <h>%d <i>documents.", countCollCurrent, countCollTotal, collname, ids.size());
			
			// Do a load check to verify we have access to this folder.
			if (ids.size() > 0 && fromDriver.load(fromColl, ids.iterator().next()) == null)
			{
				msg("<b>Skipping <h>%s <b>since could not load data.", collname);
				continue;
			}
			
			for (String id : ids)
			{
				Entry<JsonElement, Long> data = fromDriver.load(fromColl, id);
				toDriver.save(toColl, id, data.getKey());
			}
		}
		long after = System.currentTimeMillis();
		long duration = after - before;
		msg("<g>The copy is now complete. <i>It took <h>%dms<i>.", duration);
	}
	
}
