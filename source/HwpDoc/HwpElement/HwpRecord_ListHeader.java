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
package HwpDoc.HwpElement;

import java.util.logging.Logger;

import HwpDoc.Exception.HwpParseException;
import HwpDoc.paragraph.Ctrl_Common.VertAlign;

public class HwpRecord_ListHeader extends HwpRecord {
	private static final Logger log = Logger.getLogger(HwpRecord_ListHeader.class.getName());

	HwpRecord_ListHeader(int tagNum, int level, int size) {
		super(tagNum, level, size);
	}
	
	public static int getCount(int tagNum, int level, int size, byte[] buf, int off, int version) throws HwpParseException {
		int offset = off;
		
		short nParas 		= (short) (buf[offset+1]<<8&0xFF00 | buf[offset]&0x00FF);
		offset += 2;
		
		log.fine("                                                  "+"문단갯수="+nParas);
		return nParas;
	}

	public static VertAlign getVertAlign(int size, byte[] buf, int off, int version) throws HwpParseException {
		int offset = off;
		
		offset += 2;
		int attr 	 	= buf[offset+3]<<24&0xFF000000 | buf[offset+2]<<16&0x00FF0000 | buf[offset+1]<<8&0x0000FF00 | buf[offset]&0x000000FF;
		offset += 4;
		VertAlign verAlign	= VertAlign.from(attr>>21&0x03);
		
		log.fine("                                                  "
				+"리스트헤더속성="+String.format("0x%X", attr)
				+",세로정렬="+verAlign.toString());
		return verAlign;
	}

}
