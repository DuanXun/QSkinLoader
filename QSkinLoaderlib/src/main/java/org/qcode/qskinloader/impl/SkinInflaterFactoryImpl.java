package org.qcode.qskinloader.impl;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import org.qcode.qskinloader.R;
import org.qcode.qskinloader.attrhandler.SkinAttrFactory;
import org.qcode.qskinloader.base.utils.CollectionUtils;
import org.qcode.qskinloader.base.utils.Logging;
import org.qcode.qskinloader.entity.SkinAttr;
import org.qcode.qskinloader.entity.SkinAttrName;
import org.qcode.qskinloader.entity.SkinAttrSet;
import org.qcode.qskinloader.entity.SkinConstant;

import java.util.ArrayList;
import java.util.List;

/***
 * 代理View的创建，解析与换肤相关的属性
 */
class SkinInflaterFactoryImpl implements LayoutInflater.Factory {

    private static final String TAG = "SkinInflaterFactoryImpl";

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        //只有View设置了skin:enable，才解析属性
        boolean isSkinEnable = attrs.getAttributeBooleanValue(
                SkinConstant.XML_NAMESPACE,
                SkinConstant.ATTR_SKIN_ENABLE,
                false);
        if (!isSkinEnable) {
            return null;
        }

        //代理创建View
        View view = createView(context, name, attrs);

        if (view == null) {
            return null;
        }

        SkinAttrSet skinAttrSet = parseSkinAttr(context, attrs);

        //recyclerview 增加处理
        if(view instanceof RecyclerView) {
            SkinAttr clearSubAttr = new SkinAttr(SkinAttrName.CLEAR_RECYCLER_VIEW);
            if(null == skinAttrSet) {
                skinAttrSet = new SkinAttrSet(clearSubAttr);
            } else {
                skinAttrSet.addSkinAttr(clearSubAttr);
            }
        }

        if (null != skinAttrSet) {
            //将SkinItem存储在View的tag内
            view.setTag(R.id.tag_skin_attr, skinAttrSet);

            //如果有drawShadow属性，则替换ImageView为其他View
//            ShadowAttr2 skinAttrShadow = skinAttrSet.findSkinAttr(ShadowAttr2.class);
//            if (null != skinAttrShadow) {
//                if(view instanceof ImageView) {
//                    //
//                    view = createShadowImageView(context);
//                    //将SkinItem存储在View的tag内
//                    view.setTag(R.id.tag_skin_attr, skinAttrSet);
//                } else {
//                    view = createFrameWrapper(view, skinAttrShadow);
//                }
//            }
        }

        return view;
    }

//    private View createFrameWrapper(View view, ShadowAttr2 skinAttrShadow) {
//        Context context = view.getContext();
//        FrameLayout wrapperView = new FrameLayout(context);
//        FrameLayout.LayoutParams paramView = new FrameLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT);
//        wrapperView.addView(view, paramView);
//
//        View shadowView = new View(context);
//        shadowView.setVisibility(View.GONE);
//        FrameLayout.LayoutParams paramShadow = new FrameLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT);
//        wrapperView.addView(shadowView, paramShadow);
//
//        skinAttrShadow.setShadowView(shadowView);
//        return wrapperView;
//    }

//    private ShadowImageView createShadowImageView(Context context) {
//        return new ShadowImageView(context);
//    }

    /***
     * 根据名称创建view
     *
     * @param context
     * @param name
     * @param attrs
     * @return
     */
    private View createView(Context context,
                            String name,
                            AttributeSet attrs) {
        View view = null;
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            if (-1 == name.indexOf('.')) {
                if ("View".equals(name)) {
                    view = inflater.createView(
                            name, "android.view.", attrs);
                }
                if (view == null) {
                    view = inflater.createView(
                            name, "android.widget.", attrs);
                }
                if (view == null) {
                    view = inflater.createView(
                            name, "android.webkit.", attrs);
                }
            } else {
                view = inflater.createView(name, null, attrs);
            }

        } catch (Exception ex) {
            Logging.d(TAG, "createView()| create view failed", ex);
            view = null;
        }

        return view;
    }

    /***
     * 收集与换肤相关的属性
     *
     * @param context
     * @param attrs
     */
    private SkinAttrSet parseSkinAttr(Context context,
                                      AttributeSet attrs) {
        List<SkinAttr> viewAttrs = new ArrayList<SkinAttr>();

        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String attrName = attrs.getAttributeName(i);
            String attrValue = attrs.getAttributeValue(i);

            if (!SkinAttrFactory.isSupportedAttr(attrName)) {
                continue;
            }

            if (!attrValue.startsWith("@")) {
                Logging.d(TAG, "parseSkinAttr()| only support ref id");
                continue;
            }

            SkinAttr skinAttr = null;
            try {
                skinAttr = getSkinAttrFromId(context, attrName, attrValue);
            } catch (NumberFormatException ex) {
//                Logging.d(TAG, "parseSkinAttr()| error happened", ex);
                skinAttr = getSkinAttrBySplit(context, attrName, attrValue);
            } catch (NotFoundException ex) {
                Logging.d(TAG, "parseSkinAttr()| error happened", ex);
            }

            if (skinAttr != null) {
                viewAttrs.add(skinAttr);
            }
        }

        if (CollectionUtils.isEmpty(viewAttrs)) {
            return null;
        }

        return new SkinAttrSet(viewAttrs);
    }

    private SkinAttr getSkinAttrBySplit(Context context, String attrName, String attrValue) {
        try {
            int dividerIndex = attrValue.indexOf("/");
            String entryName = attrValue.substring(dividerIndex + 1, attrValue.length());
            String typeName = attrValue.substring(1, dividerIndex);
            int id = context.getResources().getIdentifier(entryName, typeName, context.getPackageName());
            return SkinAttrFactory.newSkinAttr(attrName, id, entryName, typeName);
        } catch (NotFoundException e) {
            Logging.d(TAG, "parseSkinAttr()| error happened", e);
        }
        return null;
    }

    private SkinAttr getSkinAttrFromId(Context context, String attrName, String attrValue) {
        int id = Integer.parseInt(attrValue.substring(1));
        String entryName = context.getResources().getResourceEntryName(id);
        String typeName = context.getResources().getResourceTypeName(id);
        return SkinAttrFactory.newSkinAttr(attrName, id, entryName, typeName);
    }
}
