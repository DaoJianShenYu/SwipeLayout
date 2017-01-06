package com.gyf.swipelayoutdemo2;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by 高烨峰 on 2017/1/5.
 * 侧滑菜单三步走
 * 1.一个自定义的帧布局容器,摆放前后景控件位置
 * 2.使用ViewDragHelper来处理触摸事件
 * 3.处理松手的动画事件
 * 注意:帧布局中第一个是后布局删除条目,第二个是前布局详情条目
 */
public class SwipeLayout extends FrameLayout {

	/**
	 * 三种状态
	 */
	private interface Status {
		int Close = 0;
		int Open = 1;
		//正在滑动
		int Swiping = 2;
	}

	/**
	 * 条目状态监听器
	 */
	public interface OnSwipeListener{
		void onClose(SwipeLayout layout);
		void onOpen(SwipeLayout layout);
		
		void onStartOpen(SwipeLayout layout);
		void onStartClose(SwipeLayout layout);
	}
	//默认条目状态为关闭
	private int status = Status.Close;
	private OnSwipeListener onSwipeListener;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public OnSwipeListener getOnSwipeListener() {
		return onSwipeListener;
	}

	public void setOnSwipeListener(OnSwipeListener onSwipeListener) {
		this.onSwipeListener = onSwipeListener;
	}

	private ViewDragHelper mHelper;
	
	public SwipeLayout(Context context) {
		this(context, null,0);
	}

	public SwipeLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SwipeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		// 1. 创建ViewDragHelper
		mHelper = ViewDragHelper.create(this, callback);
	}

	// 2. 转交触摸事件拦截判断, 处理触摸事件
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return mHelper.shouldInterceptTouchEvent(ev);
	};

	//2.将Touch事件转交给ViewDragHelper
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//多点触控有个概率性bug,预防下
		try {
			mHelper.processTouchEvent(event);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	// 3. 重写ViewDragHelper回调的方法
	ViewDragHelper.Callback callback = new ViewDragHelper.Callback() {
		//决定是否可以被拖拽
		@Override
		public boolean tryCaptureView(View child, int pointerId) {
			return true;
		}

		//根据可拖拽范围来计算动画时长
		public int getViewHorizontalDragRange(View child) {
			return mRange;
		};

		//决定要移动的位置,left是最新水平位置,dx是变化量
		public int clampViewPositionHorizontal(View child, int left, int dx) {
			//请求父类不拦截事件,防止listview吃掉上下滑动事件
			requestDisallowInterceptTouchEvent(true);
			// 返回的值决定了将要移动到的位置.
			if(child == mFrontView){
				if(left < - mRange){
					// 限定左范围
					return - mRange;
				}else if (left > 0) {
					// 限定右范围
					return 0;
				}
			}else if (child == mBackView) {
				if(left < mWidth - mRange){
					// 限定左范围
					return mWidth - mRange;
				}else if (left > mWidth) {
					// 限定右范围
					return mWidth;
				}
			}
			return left;
		};
		
		// 位置发生改变的时候, 把水平方向的偏移量传递给另一个布局,left 最新的水平位置
		public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
			if(changedView == mFrontView){
				// 拖拽的是前布局,  把刚刚发生的 偏移量dx 传递给 后布局
				mBackView.offsetLeftAndRight(dx);
			} else if (changedView == mBackView) {
				// 拖拽的是后布局,  把刚刚发生的 偏移量dx 传递给 前布局
				mFrontView.offsetLeftAndRight(dx);
			}
			//更新状态及调用监听
			dispatchDragEvent();
			//兼容低版本重绘界面
			invalidate();
		};
		
		public void onViewReleased(View releasedChild, float xvel, float yvel) {
			// 松手时候会被调用
			
			// xvel 向右+, 向左-
			if(xvel == 0 && mFrontView.getLeft() < - mRange * 0.5f){
				open();
			}else if(xvel < 0){
				open();
			}else {
				close();
			}
		};
	};
	private View mBackView;
	private View mFrontView;
	private int mRange;
	private int mWidth;
	private int mHeight;

	/**
	 * 更新当前的状态
	 */
	protected void dispatchDragEvent() {
		
		
		int lastStatus = status;
		// 获取最新的状态
		status = updateStatus();
		
		// 状态改变的时候, 调用监听里的方法
		if(lastStatus != status && onSwipeListener != null){
			if(status == Status.Open){
				onSwipeListener.onOpen(this);
			}else if (status == Status.Close) {
				onSwipeListener.onClose(this);
			}else if (status == Status.Swiping) {
				if(lastStatus == Status.Close){
					onSwipeListener.onStartOpen(this);
				}else if (lastStatus == Status.Open) {
					onSwipeListener.onStartClose(this);
				}
			}
		}
		
		
		
	}

	private int updateStatus() {
		int left = mFrontView.getLeft();
		if(left == 0){
			return Status.Close;
		}else if (left == -mRange) {
			return Status.Open;
		}
		
		return Status.Swiping;
	}

	//重绘时 computeScroll()方法会被调用
	@Override
	public void computeScroll() {
		super.computeScroll();
		// 维持平滑动画继续，返回 true 表示还需要重绘
		if(mHelper.continueSettling(true)){
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}
	
	/**
	 * 关闭
	 */
	protected void close() {
		close(true);
	}

	/**
	 * 是否平滑关闭
	 * @param isSmooth true为是
     */
	public void close(boolean isSmooth){
		
		int finalLeft = 0;
		if(isSmooth){
			// 触发平滑动画
			if(mHelper.smoothSlideViewTo(mFrontView, finalLeft, 0)){
				ViewCompat.postInvalidateOnAnimation(this);
			}
			
		}else {
			layoutContent(false);
		}
	}

	/**
	 * 打开
	 */
	protected void open() {
		open(true);
	}
	public void open(boolean isSmooth){
		
		int finalLeft = -mRange;
		if(isSmooth){
			//mHelper.smoothSlideViewTo(child, finalLeft, finalTop)开启一个平滑动画将 child
			//移动到 finalLeft,finalTop 的位置上。此方法返回 true 说明当前位置不是最终位置需要重绘
			if(mHelper.smoothSlideViewTo(mFrontView, finalLeft, 0)){
				//调用重绘方法
				//invalidate();可能会丢帧,此处推荐使用 ViewCompat.postInvalidateOnAnimation()
				//参数一定要传 child 所在的容器，因为只有容器才知道 child 应该摆放在什么位置
				ViewCompat.postInvalidateOnAnimation(this);
			}
			
		}else {
			layoutContent(false);
		}
	}


	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		// 默认是关闭状态
		layoutContent(false);
	}

	/**
	 * 根据当前的开启状态摆放内容
	 * @param isOpen
	 */
	private void layoutContent(boolean isOpen) {
		// 设置前布局位置
		Rect rect = computeFrontRect(isOpen);
		mFrontView.layout(rect.left, rect.top, rect.right, rect.bottom);
		// 根据前布局位置设置后布局位置
		Rect backRect = computeBackRectViaFront(rect);
		mBackView.layout(backRect.left, backRect.top, backRect.right, backRect.bottom);
		
		// 把任意布局顺序调整到最上
		bringChildToFront(mFrontView);
	}

	/**
	 * 计算后布局的矩形区域
	 * @param rect
	 * @return
	 */
	private Rect computeBackRectViaFront(Rect rect) {
		int left = rect.right;
		return new Rect(left, 0, left + mRange , 0 + mHeight);
	}

	/**
	 * 计算前布局的矩形区域
	 * @param isOpen
	 * @return
	 */
	private Rect computeFrontRect(boolean isOpen) {
		int left = 0;
		if(isOpen){
			left = -mRange;
		}
		return new Rect(left, 0, left + mWidth, 0 + mHeight);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		mRange = mBackView.getMeasuredWidth();
		
		mWidth = getMeasuredWidth();
		mHeight = getMeasuredHeight();
		
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		mBackView = getChildAt(0);
		mFrontView = getChildAt(1);
		
	}

}
