package ru.l2gw.gameserver.model.entity.fieldcycle;

import ru.l2gw.gameserver.instancemanager.FieldCycleManager;

/**
 * @author: rage
 * @date: 11.12.11 19:17
 */
public class FieldTasks
{
	public static class StepIntervalTask implements Runnable
	{
		private FieldStep fieldStep;

		public StepIntervalTask(FieldStep step)
		{
			this.fieldStep = step;
		}

		@Override
		public void run()
		{
			if(FieldCycleManager.getStep(fieldStep.getFieldId()) == fieldStep.getStep())
				FieldCycleManager.addPoint("INTERVAL", fieldStep.getFieldId(), fieldStep.getIntervalPoint());
		}
	}

	public static class DropTimeTask implements Runnable
	{
		private FieldStep fieldStep;
		private FieldCycle fieldCycle;

		public DropTimeTask(FieldCycle fieldCycle, FieldStep step)
		{
			this.fieldCycle = fieldCycle;
			this.fieldStep = step;
		}

		@Override
		public void run()
		{
			if(fieldCycle.getStep() == fieldStep.getStep())
			{
				if(fieldCycle.getLastPointChangeTime() + fieldStep.getDropTime() < System.currentTimeMillis())
					fieldCycle.setStep("DROP_TIME", Math.max(fieldStep.getStep() - 1, 0), null);
				else
					fieldCycle.startDropTask();
			}
		}
	}

	public static class ExpireTask implements Runnable
	{
		private FieldStep fieldStep;
		private FieldCycle fieldCycle;

		public ExpireTask(FieldCycle fieldCycle, FieldStep step)
		{
			this.fieldCycle = fieldCycle;
			this.fieldStep = step;
		}

		@Override
		public void run()
		{
			if(fieldCycle.getStep() == fieldStep.getStep())
			{
				fieldCycle.onExpireTime();
			}
		}
	}
}
