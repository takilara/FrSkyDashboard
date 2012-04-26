package biz.onomato.frskydash;

/**
 * Stack of values used for calculating moving average and other filtering options
 * 
 * @author Espen Solbu
 *
 */
public class MyStack {
	
//	/**
//	 * current size of the stack
//	 */
//	private int _size;
	private int getSize(){
		return _stack.length;
	}
	
	/**
	 * an array of values
	 */
	private double[] _stack;
	
	/**
	 * ctor 
	 * @param Size
	 */
	public MyStack(int Size)
	{
		_stack = new double[Size];
//		_size=Size;
		reset();
	}
	
	@Override
	public String toString()
	{
		String buf ="";
		for(int n=0;n<getSize();n++)
		{
			buf +=_stack[n];
			if(n!=getSize()-1)
			{
				buf += ",";
			}
		}
		return buf;
	}
	
	/**
	 * set all values in this stack to -1
	 * 
	 */
	public void reset()
	{
		for(int n=0;n<getSize();n++)
		{
			_stack[n] = -1;
		}
	}
	
	/**
	 * add an element to the stack. Note that this stack has a fixed size so
	 * this will push a value and drop the oldest one in return
	 * 
	 * @param item
	 * @return
	 */
	public double push(double item)
	{
		for(int n=getSize()-2;n>=0;n--)
		{
			_stack[n+1] = _stack[n];
		}
		_stack[0] = item;
		return average();
	}
	
	/**
	 * get average for this stack
	 * 
	 * @return
	 */
	public double average()
	{
		int _cnt = 0;
		double _sum = 0;
		for(int n=0;n<_stack.length;n++)
		{
			if(_stack[n]>=0)
			{
				_cnt++;
				_sum += _stack[n];
			}
		}
		double _avg = _sum/_cnt;
		return _avg;
	}
}
	

