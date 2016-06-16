package luft27.androidbeginner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by amalikov on 16.06.2016.
 */
public class GameView extends View {
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(width / 2, height / 2, 10, paint);
    }

    private final Paint paint;
    private int width;
    private int height;
}
