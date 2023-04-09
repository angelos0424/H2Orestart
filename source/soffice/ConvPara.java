/* MIT License
 *  
 * Copyright (c) 2022 ebandal
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * 본 제품은 한글과컴퓨터의 ᄒᆞᆫ글 문서 파일(.hwp) 공개 문서를 참고하여 개발하였습니다.
 * 개방형 워드프로세서 마크업 언어(OWPML) 문서 구조 KS X 6101:2018 문서를 참고하였습니다.
 * 작성자 : 반희수 ebandal@gmail.com  
 * 작성일 : 2022.10
 */
package soffice;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.star.awt.FontRelief;
import com.sun.star.awt.FontSlant;
import com.sun.star.awt.FontStrikeout;
import com.sun.star.awt.FontUnderline;
import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.style.LineSpacing;
import com.sun.star.style.LineSpacingMode;
import com.sun.star.style.ParagraphAdjust;
import com.sun.star.style.TabAlign;
import com.sun.star.style.TabStop;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.table.ShadowFormat;
import com.sun.star.table.ShadowLocation;
import com.sun.star.text.FontEmphasis;
import com.sun.star.text.ParagraphVertAlign;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import HwpDoc.HwpDocInfo.CompatDoc;
import HwpDoc.HwpElement.HwpRecord_BorderFill;
import HwpDoc.HwpElement.HwpRecord_CharShape;
import HwpDoc.HwpElement.HwpRecord_ParaShape;
import HwpDoc.HwpElement.HwpRecord_Style;
import HwpDoc.HwpElement.HwpRecord_TabDef;
import HwpDoc.HwpElement.HwpRecord_CharShape.Outline;
import HwpDoc.HwpElement.HwpRecord_CharShape.Shadow;
import HwpDoc.HwpElement.HwpRecord_TabDef.Tab;

public class ConvPara {
	private static final Logger log = Logger.getLogger(ConvPara.class.getName());
	private static Map<Integer, String> paragraphStyleNameMap = new HashMap<Integer, String>();
	private static final String PARAGRAPH_STYLE_PREFIX = "HWP ";

	static final double PARABREAK_SPACING = 0.91;
    static final double PARA_SPACING = 0.87;
    
	public static void reset(WriterContext wContext) {
		deleteCustomStyles(wContext);
	}
	
	private static void deleteCustomStyles(WriterContext wContext) {
		if (wContext.mMyDocument!=null) {
			try {
		        XStyleFamiliesSupplier xSupplier = (XStyleFamiliesSupplier)UnoRuntime.queryInterface(XStyleFamiliesSupplier.class, wContext.mMyDocument);
		        XNameAccess xFamilies = (XNameAccess) UnoRuntime.queryInterface (XNameAccess.class, xSupplier.getStyleFamilies());
		        
		        XNameContainer xParagraphFamily = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, xFamilies.getByName("ParagraphStyles"));
		        for (Integer custIndex: paragraphStyleNameMap.keySet()) {
		        	log.info("Deleting "+paragraphStyleNameMap.get(custIndex));
		        	if (xParagraphFamily.hasByName(paragraphStyleNameMap.get(custIndex))) {
		        		try {
		        			xParagraphFamily.removeByName(paragraphStyleNameMap.get(custIndex));
		        		} catch (com.sun.star.lang.DisposedException e) {
		        			e.printStackTrace();
		        		}
		        	}
		        }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		paragraphStyleNameMap.clear();
	}

	public static void makeCustomParagraphStyle(WriterContext wContext, int id, HwpRecord_Style hwpStyle) {
		
		HwpRecord_ParaShape paraShape = wContext.getParaShape(hwpStyle.paraShape);
		HwpRecord_CharShape charShape = wContext.getCharShape(hwpStyle.charShape);
		
		try {
			XStyle xListStyle = UnoRuntime.queryInterface(XStyle.class, wContext.mMSF.createInstance("com.sun.star.style.ParagraphStyle"));
			XStyleFamiliesSupplier xSupplier = (XStyleFamiliesSupplier)UnoRuntime.queryInterface(XStyleFamiliesSupplier.class, wContext.mMyDocument);
			XNameAccess xFamilies = (XNameAccess) UnoRuntime.queryInterface (XNameAccess.class, xSupplier.getStyleFamilies());
			XNameContainer xFamily = (XNameContainer) UnoRuntime.queryInterface(XNameContainer.class, xFamilies.getByName("ParagraphStyles"));
			
			String hwpStyleName = PARAGRAPH_STYLE_PREFIX +" "+id+" "+ hwpStyle.name;
			if (xFamily.hasByName(hwpStyleName)==false) {
				xFamily.insertByName (hwpStyleName, xListStyle);
			}
		   	paragraphStyleNameMap.put(id, hwpStyleName);
		   	
			XPropertySet xStyleProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xFamily.getByName(hwpStyleName));
			
			setParagraphProperties(xStyleProps, paraShape, wContext.getDocInfo().compatibleDoc, PARA_SPACING);
			setCharacterProperties(xStyleProps, charShape, 1);

			// NumberingRules 속성을 설정해야  Style이 변경된다. 
	        XPropertySet xCursorProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, wContext.mTextCursor);
            xCursorProps.setPropertyValue("ParaStyleName", "Standard");
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static void setNumberingProperties(XPropertySet xStyleProps, HwpRecord_ParaShape paraShape) {
		String numberingStyleName = "";
		try {
			switch(paraShape.headingType) {
			case NONE:
				xStyleProps.setPropertyValue("NumberingStyleName", "default");
				xStyleProps.setPropertyValue("NumberingLevel", (short) 0);
				break;
			case OUTLINE:
				numberingStyleName = ConvNumbering.getOutlineStyleName();
				xStyleProps.setPropertyValue("NumberingStyleName", numberingStyleName);
				xStyleProps.setPropertyValue("NumberingLevel", (short) (paraShape.headingLevel));
				break;
			case NUMBER:
				log.finest("번호문단ID="+paraShape.headingIdRef + ",문단수준="+paraShape.headingLevel);
				numberingStyleName = ConvNumbering.numberingStyleNameMap.get((int)paraShape.headingIdRef);
				xStyleProps.setPropertyValue("NumberingStyleName", numberingStyleName);
				xStyleProps.setPropertyValue("NumberingLevel", (short) paraShape.headingLevel);
				break;
			case BULLET:
				log.finest("글머리표문단ID="+paraShape.headingIdRef + ",문단수준="+paraShape.headingLevel);
				numberingStyleName = ConvNumbering.bulletStyleNameMap.get((int)paraShape.headingIdRef);
				xStyleProps.setPropertyValue("NumberingStyleName", numberingStyleName);
				xStyleProps.setPropertyValue("NumberingLevel", (short) 0);
				break;
			}
			
			// 문단번호를 설정하면  들여쓰기가 엉망이된다. redundancy 같지만, 들여쓰기를 위해  Margin 설정값을 넣는다.
			xStyleProps.setPropertyValue("ParaLeftMargin", Transform.translateHwp2Office(paraShape.marginLeft/2));
			xStyleProps.setPropertyValue("ParaRightMargin", Transform.translateHwp2Office(paraShape.marginRight/2));
			xStyleProps.setPropertyValue("ParaTopMargin", Transform.translateHwp2Office(paraShape.marginPrev/2));
			xStyleProps.setPropertyValue("ParaBottomMargin", Transform.translateHwp2Office(paraShape.marginNext/2));
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	static void setParagraphProperties(XPropertySet xStyleProps, HwpRecord_ParaShape paraShape, CompatDoc compat, double preferSpace) {
		try {
			ParagraphAdjust align = ParagraphAdjust.BLOCK;
			switch(paraShape.align) {
			case LEFT:
				align = ParagraphAdjust.LEFT;
				break;
			case RIGHT:
				align = ParagraphAdjust.RIGHT;
				break;
			case CENTER:
				align = ParagraphAdjust.CENTER;
				break;
			case JUSTIFY:
			case DISTRIBUTE:
			case DISTRIBUTE_SPACE:
				align = ParagraphAdjust.BLOCK;
				break;
			}
			xStyleProps.setPropertyValue("ParaAdjust", align);
			// breakLatinWord		// 줄 나눔 기준 영어 단위 (0:단어, 1:하이픈, 2:글자)
			// breakNonLatinWord	// 줄 나눔 기준 한글 단위 (0:어절, 1:글자)
			// snapToGrid			// 편집 용지의 줄 격자 사용 여부
			// condense				// 공백 최소값 (0%~75%)
			// widowOrphan			// 외톨이줄 보호 여부
			// keepWithNext			// 다음 문단과 함께 여부
			xStyleProps.setPropertyValue("ParaKeepTogether", paraShape.keepWithNext);
			// pageBreakBefore		// 문단 앞에서 항상 쪽 나눔 여부
			// verAlign				// 세로정렬 (0:글꼴기준, 1:위쪽, 2:가운데, 3:아래)
			short vertAlign = ParagraphVertAlign.CENTER;
			switch(paraShape.vertAlign) {
			case BASELINE:
				vertAlign = ParagraphVertAlign.BASELINE;
				break;
			case TOP:
				vertAlign = ParagraphVertAlign.TOP;
				break;
			case CENTER:
				vertAlign = ParagraphVertAlign.CENTER;
				break;
			case BOTTOM:
				vertAlign = ParagraphVertAlign.BOTTOM;
				break;
			}
			xStyleProps.setPropertyValue("ParaVertAlignment", vertAlign);
			// fontLineHeight		// 글꼴에 어울리는 줄 높이 여부
			// HeadingType	headingType	// 문단 머리 모양 종류 (0:없음, 1:개요, 2:번호, 3:글머리표(bullet))
			// heading;				// 번호 문단 ID(Numbering ID) 또는 글머리표 문단 모양 ID(Bullet ID)참조 값
			// headingLevel			// 문단 수준 (1수준~7수준)
			/*
			String numberingStyleName = "";
			switch(paraShape.headingType) {
			case NONE:
				xStyleProps.setPropertyValue("NumberingStyleName", "default");
				// xStyleProps.setPropertyValue("NumberingLevel", (short) 0);
				break;
			case OUTLINE:
				numberingStyleName = ConvNumbering.getOutlineStyleName();
				xStyleProps.setPropertyValue("NumberingStyleName", numberingStyleName);
				// xStyleProps.setPropertyValue("NumberingLevel", (short) (paraShape.headingLevel+1));
				break;
			case NUMBER:
				log.finest("번호문단ID="+paraShape.heading + ",문단수준="+paraShape.headingLevel);
				numberingStyleName = ConvNumbering.numberingStyleNameMap.get((int)paraShape.heading);
				xStyleProps.setPropertyValue("NumberingStyleName", numberingStyleName);
				// xStyleProps.setPropertyValue("NumberingLevel", (short) paraShape.headingLevel);
				break;
			case BULLET:
				log.finest("글머리표문단ID="+paraShape.heading + ",문단수준="+paraShape.headingLevel);
				numberingStyleName = ConvNumbering.bulletStyleNameMap.get((int)paraShape.heading);
				xStyleProps.setPropertyValue("NumberingStyleName", numberingStyleName);
				// xStyleProps.setPropertyValue("NumberingLevel", (short) 0);
				break;
			}
			*/
			// connect				// 문단 테두리 연결 여부
			xStyleProps.setPropertyValue("ParaIsConnectBorder", paraShape.connect);
			// ignoreMargin			// 문단 여백 무시 여부
			// paraTailShape		// 문단 꼬리 모양
			// indent				// 들여쓰기/내어쓰기.   
			// 들여쓰기(+)는 첫줄을 오른쪽으로 얼마나 이동할지..  ParaFirstLineIndent 로 조정
			// 내어쓰기(-)는 두번째줄부터 오른쪽으로 얼마나 이동할지..  LeftMargin +조정하고, 첫줄 -조정
			xStyleProps.setPropertyValue("ParaIsAutoFirstLineIndent", false);
			if (paraShape.indent >= 0) {
				xStyleProps.setPropertyValue("ParaFirstLineIndent", Transform.translateHwp2Office(paraShape.indent/2));
				// marginLeft			// 왼쪽 여백
				xStyleProps.setPropertyValue("ParaLeftMargin", Transform.translateHwp2Office(paraShape.marginLeft/2));
			} else {
				xStyleProps.setPropertyValue("ParaFirstLineIndent", Transform.translateHwp2Office(paraShape.indent/2));
				// marginLeft			// 왼쪽 여백
				xStyleProps.setPropertyValue("ParaLeftMargin", Transform.translateHwp2Office(paraShape.marginLeft/2-paraShape.indent/2));
			}
			
			// marginRight			// 오른쪽 여백
			xStyleProps.setPropertyValue("ParaRightMargin", Transform.translateHwp2Office(paraShape.marginRight/2));
			// marginPrev			// 문단 간격 위
			xStyleProps.setPropertyValue("ParaTopMargin", Transform.translateHwp2Office(paraShape.marginPrev/2));
			// marginNext			// 문단 간격 아래
			xStyleProps.setPropertyValue("ParaBottomMargin", Transform.translateHwp2Office(paraShape.marginNext/2));
			// lineSpacing			// 줄 간격. 한글2007 이하버전(5.0.2.5 버전 미만)에서 사용.
			// 							percent일때:0%~500%, fixed일때:hpwunit또는 글자수,betweenline일때:hwpunit또는글자수
			// lineSpacingType;		// 줄간격 종류(0:Percent,1:Fixed,2:BetweenLines,4:AtLeast)
			LineSpacing lineSpacing = new LineSpacing();
			switch(paraShape.lineSpacingType) {
			case 0x0:
				lineSpacing.Mode = LineSpacingMode.PROP;
				// 일반텍스트에서는 lineSpacing을 줄인다. HWP 24pt=8.5mm, LO 24pt=11mm, so delta=2.5/11=22.7%
				// 텍스트 상자 내, 테이블 내에서는  lineSpacing을 그대로 반영.
	            double scale = 1.0;
				switch(compat) {
				case HWP:
	                scale = preferSpace>0.0?preferSpace:PARA_SPACING;
	                break;
				case MS_WORD:
                    scale = 1.21;
                    break;
                case OLD_HWP:
                default:
                    scale = 1.0;
				}
				lineSpacing.Height = (short)(paraShape.lineSpacing*scale);
				break;
			case 0x1:
				lineSpacing.Mode = LineSpacingMode.FIX;
				lineSpacing.Height = (short)(paraShape.lineSpacing/2*0.352778);	//예) 값:4600, 한컴:23pt, LO:8.113894mm. (1pt=0.352778mm)
				break;
			case 0x2:
				lineSpacing.Mode = LineSpacingMode.LEADING;
				lineSpacing.Height = (short)(paraShape.lineSpacing);	//
				break;
			case 0x3:
				lineSpacing.Mode = LineSpacingMode.MINIMUM; 
				lineSpacing.Height = (short)(paraShape.lineSpacing);	//
				break;
			}
			log.finest("lineSpacing="+lineSpacing.Height+"("+lineSpacing.Mode+") <= LineSpacing="+paraShape.lineSpacing + "("+paraShape.lineSpacingType+")");
			
			xStyleProps.setPropertyValue("ParaLineSpacing", lineSpacing);
			// tabDef				// 탭 정의 아이디(TabDef ID) 참조 값
			HwpRecord_TabDef tabDef = WriterContext.getTabDef(paraShape.tabDef);
			TabStop[] tss = new TabStop[tabDef.count];
			if (tabDef.count>0) {
				for (int i=0; i<tabDef.count; i++) {
			  		tss[i] = new TabStop();
			  		Tab tab = tabDef.tabs.get(i);
			  		switch(tab.type) {
			  		case LEFT:
			  			tss[i].Alignment = TabAlign.LEFT;
			  			break;
			  		case RIGHT:
			  			tss[i].Alignment = TabAlign.RIGHT;
			  			break;
			  		case CENTER:
			  			tss[i].Alignment = TabAlign.CENTER;
			  			break;
			  		case DECIMAL:
			  			tss[i].Alignment = TabAlign.DECIMAL;
						tss[i].DecimalChar = 46;
			  			break;
			  		}
			  		switch(tab.leader) {
					case NONE:
						tss[i].FillChar = 32;
						break;
					case SOLID:
					case DASH:
					case DOT:
					case DASH_DOT:
					case DASH_DOT_DOT:
					case LONG_DASH:
						tss[i].FillChar = 45;
						break;
					default:
						tss[i].FillChar = 45;
						break;
					}
					tss[i].Position = Math.min(Transform.translateHwp2Office(tab.pos/200), 150)*100;	// 15cm
				}
			} else {
				if ((tabDef.attr&0x2)==0x2) {			// 문단 오른쪽 끝 자동 탭
			  		tss = new TabStop[1];
		  			tss[0] = new TabStop();
		  			HwpDoc.section.Page page = ConvPage.getCurrentPage().page;
					tss[0].Position = Transform.translateHwp2Office(page.width-page.marginLeft-page.marginRight); // 150*100;
					tss[0].Alignment = TabAlign.RIGHT;
					tss[0].FillChar = 32;
				} else if ((tabDef.attr&0x1)==0x1) {	// 내어쓰기용 자동 탭
			  		tss = new TabStop[1];
		  			tss[0] = new TabStop();
					tss[0].Position = 0;
					tss[0].Alignment = TabAlign.LEFT;
					tss[0].FillChar = 32;
				}
			}
			xStyleProps.setPropertyValue("ParaTabStops", tss);
			// borderFill			// 테두리/배경 모양 ID(BorderFill ID) 참조 값
			HwpRecord_BorderFill borders = WriterContext.getBorderFill(paraShape.borderFill);
			if (borders!=null) {
				xStyleProps.setPropertyValue("LeftBorder", Transform.toBorderLine(borders.left));
				xStyleProps.setPropertyValue("RightBorder", Transform.toBorderLine(borders.right));
				xStyleProps.setPropertyValue("TopBorder", Transform.toBorderLine(borders.top));
				xStyleProps.setPropertyValue("BottomBorder", Transform.toBorderLine(borders.bottom));
				if (borders.fill!=null && borders.fill.isColorFill()==true && borders.fill.faceColor!=-1) {
					xStyleProps.setPropertyValue("ParaBackColor", borders.fill.faceColor);
				}
			}
			// offsetLeft			// 문단 테두리 왼쪽 간격
			xStyleProps.setPropertyValue("LeftBorderDistance", Transform.translateHwp2Office(paraShape.offsetLeft));
			// offsetRight			// 문단 테두리 오른쪽 간격
			xStyleProps.setPropertyValue("RightBorderDistance", Transform.translateHwp2Office(paraShape.offsetRight));
			// offsetTop			// 문단 테두리 위쪽 간격
			xStyleProps.setPropertyValue("TopBorderDistance", Transform.translateHwp2Office(paraShape.offsetTop));
			// offsetBottom			// 문단 테두리 아래쪽 간격
			xStyleProps.setPropertyValue("BottomBorderDistance", Transform.translateHwp2Office(paraShape.offsetBottom));
									// 속성2 (5.0.1.7 버전 이상)
			// lineWrap;			// 		한줄로 입력
			// autoSpaceEAsianEng;						// 		한글과 영어 간격을 자동 조절
			// autoSpaceEAsianNum;						// 		한글과 숫자 간격을 자동 조절
			
			//	https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1style_1_1ParagraphProperties.html
			//	ParaAdjust,ParaLineSpacing,ParaBackColor,ParaBackTransparent,ParaBackGraphicURL,ParaBackGraphicFilter,ParaBackGraphicLocation,ParaLastLineAdjust,
			//	ParaExpandSingleWord,ParaLeftMargin,ParaRightMargin,ParaTopMargin,ParaBottomMargin,ParaContextMargin,ParaLineNumberCount,ParaLineNumberStartValue,
			//	PageDescName,PageNumberOffset,ParaRegisterModeActive,ParaStyleName,PageStyleName,DropCapFormat,DropCapWholeWord,ParaKeepTogether,ParaSplit,
			//	NumberingLevel,NumberingRules,NumberingStartValue,ParaIsNumberingRestart,NumberingStyleName,ParaOrphans,ParaWidows,ParaShadowFormat,LeftBorder,
			//	RightBorder,TopBorder,BottomBorder,BorderDistance,LeftBorderDistance,RightBorderDistance,TopBorderDistance,BottomBorderDistance,BreakType,
			//	DropCapCharStyleName,ParaFirstLineIndent,ParaIsAutoFirstLineIndent,ParaIsHyphenation,ParaHyphenationMaxHyphens,ParaHyphenationMaxLeadingChars,
			//	ParaHyphenationMaxTrailingChars,ParaVertAlignment,ParaUserDefinedAttributes,NumberingIsNumber,ParaIsConnectBorder,ListId,OutlineLevel,
			//	ContinueingPreviousSubTree,ListLabelString,ParaHyphenationNoCaps,
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void setCharacterProperties(XPropertySet xStyleProps, HwpRecord_CharShape charShape, int step) {
		try {
			xStyleProps.setPropertyValue("CharFontName", charShape.fontName[1]);
			// paraProps.setPropertyValue("CharFontStyleName", faceName.faceName);
			xStyleProps.setPropertyValue("CharFontNameAsian", charShape.fontName[0]);
			// paraProps.setPropertyValue("CharFontStyleNameAsian", faceName.faceName);
			
			// charShape.fontID[0];						// 언어별 글꼴ID(FaceID)		// f#
			// charShape.ratio[0];						// 언어별 장평, 50%~200%		// r#
			log.finest("CharWidth="+charShape.ratio[0]);
			xStyleProps.setPropertyValue("CharScaleWidth", charShape.ratio[0]);
			
			// charShape.spacing[0];							// 언어별 자간, -50%~50%		// s#
			// 자간 -24%~50% -> 100mm 로 변환 (-2pt~4.5pt; LibreOffice 최소는 -2pt)
			double spacing = charShape.spacing[0]*3.02330746+10.58334; // char 가로% -> mm로 변환
			xStyleProps.setPropertyValue("CharKerning", (short)Math.round(spacing));
			// charShape.relSize[0];							// 언어별 상대 크기, 10%~250%	// e#
			// charShape.charOffset[0];						// 언어별 글자 위치, -100%~100%	// o#
			// charShape.height;								// 기준 크기, 0pt~4096pt		// he
			xStyleProps.setPropertyValue("CharHeight", (float)charShape.height*(charShape.relSize[1]/100.0f)/100.0f);	// 1000 (10.0pt)
			xStyleProps.setPropertyValue("CharHeightAsian", (float)charShape.height*(charShape.relSize[0]/100.0f)/100.0f);	// 1000 (10.0pt)
			
			// charShape.bold;									// 진하게 여부					// bo
			if (charShape.bold) {
				xStyleProps.setPropertyValue("CharWeight", FontWeight.BOLD);
				xStyleProps.setPropertyValue("CharWeightAsian", FontWeight.BOLD);
			} else {
				xStyleProps.setPropertyValue("CharWeight", FontWeight.NORMAL);
				xStyleProps.setPropertyValue("CharWeightAsian", FontWeight.NORMAL);
			}
			// charShape.italic;								// 기울임 여부					// it
			if (charShape.italic) {
				xStyleProps.setPropertyValue("CharPosture", FontSlant.ITALIC);
				xStyleProps.setPropertyValue("CharPostureAsian", FontSlant.ITALIC);
			} else {
				xStyleProps.setPropertyValue("CharPosture", FontSlant.NONE);
				xStyleProps.setPropertyValue("CharPostureAsian", FontSlant.NONE);
			}
			
			// charShape.underline;							// 밑줄 종류					// ut
			// charShape.underlineShape;						// 밑줄 모양					// us
			switch(charShape.underline) {
			case NONE:
				xStyleProps.setPropertyValue("CharUnderline", FontUnderline.NONE);
				break;
			case BOTTOM:
			case CENTER:
			case TOP:
				switch (charShape.underlineShape) {
				case SOLID:
					xStyleProps.setPropertyValue("CharUnderline", FontUnderline.SINGLE);
					break;
				case DASH:
					xStyleProps.setPropertyValue("CharUnderline", FontUnderline.DASH);
					break;
				case DOT:
					xStyleProps.setPropertyValue("CharUnderline", FontUnderline.DOTTED);
					break;
				case DASH_DOT:
					xStyleProps.setPropertyValue("CharUnderline", FontUnderline.DASHDOT);
					break;
				case DASH_DOT_DOT:
					xStyleProps.setPropertyValue("CharUnderline", FontUnderline.DASHDOTDOT);
					break;
				case LONG_DASH:
					xStyleProps.setPropertyValue("CharUnderline", FontUnderline.LONGDASH);
					break;
				case DOUBLE_SLIM:
					xStyleProps.setPropertyValue("CharUnderline", FontUnderline.DOUBLE);
					break;
				case CIRCLE:
				case SLIM_THICK:
				case THICK_SLIM:
				case SLIM_THICK_SLIM:
					xStyleProps.setPropertyValue("CharUnderline", FontUnderline.SINGLE);
					break;
				default:
					break;
				}
				break;
			}
			// charShape.underlineColor;						// 밑줄 색
			xStyleProps.setPropertyValue("CharUnderlineColor", charShape.underlineColor);
			// charShape.outline;								// 외곽선종류					//
			if (charShape.outline==Outline.NONE) {
				xStyleProps.setPropertyValue("CharContoured", false);
			} else {
				xStyleProps.setPropertyValue("CharContoured", true);
			}
		
			// charShape.emboss;								// 양각 여부					// em?
			// charShape.engrave;								// 음각 여부					// en?
			if (charShape.emboss) {
				xStyleProps.setPropertyValue("CharRelief", FontRelief.EMBOSSED);
			} else if (charShape.engrave) {
				xStyleProps.setPropertyValue("CharRelief", FontRelief.ENGRAVED);
			} else {
				xStyleProps.setPropertyValue("CharRelief", FontRelief.NONE);
			}
			// charShape.superScript;							// 위 첨자 여부					// su?
			// charShape.subScript;							// 아래 첨자 여부				// sb?
			
			// charShape.strikeOut;							// 취소선 여부
			//	charShape.strikeOutShape;						// 취소선 모양
			//	charShape.strikeOutColor;						// 취소선 색
			if (charShape.strikeOut!=0) {
				switch(charShape.strikeOutShape) {
					case SOLID:
						xStyleProps.setPropertyValue("CharStrikeout", FontStrikeout.SINGLE);
			  			break;
					case DASH:
						xStyleProps.setPropertyValue("CharStrikeout", FontStrikeout.SINGLE);
			  			break;
					case DOT:
					case DASH_DOT:
					case DASH_DOT_DOT:
					case LONG_DASH:
						xStyleProps.setPropertyValue("CharStrikeout", FontStrikeout.SINGLE);
			  			break;
					case DOUBLE_SLIM:
						xStyleProps.setPropertyValue("CharStrikeout", FontStrikeout.DOUBLE);
			  			break;
					case CIRCLE:
					case SLIM_THICK:
					case THICK_SLIM:
					case SLIM_THICK_SLIM:
						xStyleProps.setPropertyValue("CharUnderline", FontUnderline.SINGLE);
						break;
					default:
			  			break;
				}
			}
		
			//charShape.symMark;								// 강조점 종류
			xStyleProps.setPropertyValue("CharEmphasis", FontEmphasis.NONE);
		
			//charShape.useFontSpace;							// 글꼴에 어울리는 빈칸 사용 여부		// uf?
			//charShape.useKerning;							// kerning여부				// uk?
			//charShape.textColor;							// 글자 색						//
			xStyleProps.setPropertyValue("CharColor", charShape.textColor);
		
			//charShape.shadeColor;							// 음영 색
			if (charShape.shadeColor != 0xFFFFFFFF) {
			    xStyleProps.setPropertyValue("CharBackColor", charShape.shadeColor);
			}

		
			//	charShape.shadow;								// 그림자 종류					// 
			//	charShape.shadowSpacing;						// 그림자 간격, -100%~100%
			//	charShape.shadowColor;							// 그림자 색
			if (charShape.shadow!=Shadow.NONE) {
				ShadowFormat sf = new ShadowFormat();
				sf.Location = ShadowLocation.BOTTOM_RIGHT;
				sf.ShadowWidth = charShape.shadowOffsetX;
				sf.IsTransparent = false;
				sf.Color = charShape.shadowColor;
				xStyleProps.setPropertyValue("CharShadowed", true);
				xStyleProps.setPropertyValue("CharShadowFormat", sf);
			}
			//	charShape.borderFillId;							// 글자 테두리/배경 ID(CharShapeBorderFill ID) 참조 값

			//	https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1style_1_1CharacterProperties.html
			//	CharFontName,CharFontStyleName,CharFontFamily,CharFontCharSet,CharFontPitch,CharColor,CharEscapement,CharHeight,CharUnderline,CharWeight,CharPosture,
			//	CharAutoKerning,CharBackColor,CharShadingValue,CharBackTransparent,CharCaseMap,CharCrossedOut,CharFlash,CharStrikeout,CharWordMode,CharKerning,CharLocale,
			//	CharKeepTogether,CharNoLineBreak,CharShadowed,CharFontType,CharStyleName,CharContoured,CharCombineIsOn,CharCombinePrefix,CharCombineSuffix,CharEmphasis
			//	CharRelief,RubyText,RubyAdjust,RubyCharStyleName,RubyIsAbove,CharRotation,CharRotationIsFitToLine,CharScaleWidth,HyperLinkURL,HyperLinkTarget,HyperLinkName
			//	VisitedCharStyleName,UnvisitedCharStyleName,CharEscapementHeight,CharNoHyphenation,CharUnderlineColor,CharUnderlineHasColor,CharHidden,TextUserDefinedAttributes
			//	CharLeftBorder,CharRightBorder,CharTopBorder,CharBottomBorder,CharBorderDistance,CharLeftBorderDistance,CharRightBorderDistance,CharTopBorderDistance
			//	CharBottomBorderDistance,CharShadowFormat,CharHighlight,RubyPosition
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	public static void setDefaultParaStyle(WriterContext wContext) {
		try {
	        XParagraphCursor xParaCursor = (XParagraphCursor) UnoRuntime.queryInterface(XParagraphCursor.class, wContext.mTextCursor);
            xParaCursor.gotoEnd(false);
	        XPropertySet xParaProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xParaCursor);
	        String styleName = getStyleName(0);
	        xParaProps.setPropertyValue ("ParaStyleName", styleName);
	        xParaProps.setPropertyValue ("NumberingStyleName", "default");
	        xParaProps.setPropertyValue ("NumberingLevel", (short) 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static String getStyleName(int styleID) {
	   	return paragraphStyleNameMap.get(styleID);
	}
}
