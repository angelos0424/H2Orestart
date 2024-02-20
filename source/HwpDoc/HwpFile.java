/* Copyright (C) 2023 ebandal
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
/* 본 제품은 한글과컴퓨터의 ᄒᆞᆫ글 문서 파일(.hwp) 공개 문서를 참고하여 개발하였습니다.
 * 개방형 워드프로세서 마크업 언어(OWPML) 문서 구조 KS X 6101:2018 문서를 참고하였습니다.
 * 작성자 : 반희수 ebandal@gmail.com  
 * 작성일 : 2022.10
 */
package HwpDoc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import HwpDoc.HwpElement.HwpRecord_BinData.Compressed;
import HwpDoc.Exception.CompoundDetectException;
import HwpDoc.Exception.CompoundParseException;
import HwpDoc.Exception.HwpParseException;
import HwpDoc.Exception.NotImplementedException;
import HwpDoc.HwpElement.HwpTag;
import HwpDoc.OLEdoc.CompoundFile;
import HwpDoc.OLEdoc.DirectoryEntry;
import HwpDoc.paragraph.HwpParagraph;

public class HwpFile {
    private static final Logger log = Logger.getLogger(HwpFile.class.getName());

    public	String filename;
    public	CompoundFile oleFile;
    public	HwpFileHeader fileHeader;
    public	int	version;
    public	HwpDocInfo 	docInfo;
    public	List<HwpSection> bodyText;
    public	List<HwpSection> viewText;
    
    // Let's have member that are needed for showing in LibreOffice
    public	List<DirectoryEntry> directoryBinData;
    public	List<HwpParagraph>  paraList;
    
    
    public HwpFile(String filename) throws FileNotFoundException {
        this.filename = filename;
        oleFile = new CompoundFile(this.filename);
        fileHeader = new HwpFileHeader();
        docInfo = new HwpDocInfo(this);
        bodyText = new ArrayList<HwpSection>();
        viewText = new ArrayList<HwpSection>();
    }

    public HwpFile(File file) throws FileNotFoundException {
        oleFile = new CompoundFile(file);
        this.filename = file.toString();
        fileHeader = new HwpFileHeader();
        docInfo = new HwpDocInfo(this);
        bodyText = new ArrayList<HwpSection>();
        viewText = new ArrayList<HwpSection>();
    }

    public List<HwpSection> getSections() {
        if (fileHeader.bDistributable) {
            return viewText;
        } else {
            return bodyText;
        }
    }
    
    public CompoundFile getOleFile() {
        return oleFile;
    }
    
    public boolean detect() throws HwpDetectException, IOException {
        // read CompoundFile structure
        try {
            oleFile.open();
            if (getFileHeader() == false) {
                oleFile.close();
                throw new HwpDetectException(ErrCode.FILE_READ_ERROR);
            }
        } catch (CompoundDetectException e) {
            oleFile.close();
            throw new HwpDetectException(e.getReason());
        } catch (HwpDetectException e) {
            oleFile.close();
            throw new HwpDetectException(e.getReason());
        }
        log.fine("Header parsed");
        return true;
    }

    public void open() throws HwpDetectException, CompoundDetectException, IOException, DataFormatException, HwpParseException, NotImplementedException, CompoundParseException {
        if (fileHeader.signature==null || fileHeader.version==null) {
            detect();
        }
        version = Integer.parseInt(fileHeader.version);
        
        if (fileHeader.bPasswordEncrypted) {
            throw new HwpParseException();
        }
        
        if (getDocInfo(version)==false) 
            throw new CompoundParseException();
        log.fine("DocInfo parsed");
        
        // 배포용 문서가 아니면 BodyText를 읽는다.
        if (fileHeader.bDistributable==false) {
            if (getBodyText(version)==false) 
                throw new CompoundParseException();
            log.fine("BodyText parsed");
        }
        
        // 배포용 문서면 ViewText를 읽는다.
        if (fileHeader.bDistributable) {
            if (getViewText(version)==false) 
                throw new CompoundParseException();
            log.fine("Distributable file. ViewText parsed");
        }
    }
    
    public void saveHwpComponent() throws IOException {
        Compressed compressed = fileHeader.bCompressed?Compressed.COMPRESS:Compressed.NO_COMPRESS;
        
        // Save internal component for debugging purpose.
        String patternStr = ".*"+Pattern.quote(File.separator)+"([^"+Pattern.quote(File.separator)+"]+)$";
        String shortFilename = filename.replaceAll(patternStr, "$1");	// ".*\\\\([^\\\\]+)$"
        shortFilename = shortFilename.replaceAll("(.*)\\.hwp$", "$1");
        log.finer("HwpFilePath="+filename + ", FileName="+shortFilename);
        File rootFolder = new File(shortFilename);
        if (!rootFolder.exists()) {
            Files.createDirectories(Paths.get(shortFilename));
        }
        saveChildEntries(Paths.get(shortFilename), "Root Entry", compressed);
    }
    
    private void saveChildEntries(Path basePath, String storageName, Compressed compressed) throws IOException {
        List<DirectoryEntry> entries = oleFile.getChildEntries(storageName);
        for (DirectoryEntry entry: entries) {
            if (entry.getObjectType()==0x01) {
                Path childPath = Paths.get(basePath.toString(), entry.getDirectoryEntryName().trim());
                if (childPath.toFile().exists()==false) {
                    Files.createDirectory(childPath);
                }
                saveChildEntries(childPath, entry.getDirectoryEntryName().trim(), compressed);
            } else {
                byte[] buf = oleFile.read(entry);
                try (FileOutputStream fos = new FileOutputStream(Paths.get(basePath.toString(), entry.getDirectoryEntryName().trim()).toFile())) {
                    if (compressed == Compressed.COMPRESS || (compressed==Compressed.FOLLOW_STORAGE && fileHeader.bCompressed)) {
                        fos.write(unzip(oleFile.read(entry)));
                    } else {
                        fos.write(oleFile.read(entry));
                    }
                } catch (DataFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private DirectoryEntry searchChildEntry(Path basePath, DirectoryEntry baseEntry, String entryName) throws IOException {
        List<DirectoryEntry> entries = oleFile.getChildEntries(baseEntry);
        for (DirectoryEntry entry: entries) {
            if (entry.getObjectType()==0x01) {
                Path childPath = Paths.get(basePath.toString(), entry.getDirectoryEntryName().trim());
                // Files.createDirectory(childPath);
                return searchChildEntry(childPath, entry, entryName);
            } else {
                if (entry.getDirectoryEntryName().trim().equals(entryName)) {
                    return entry;
                }
            }
        }
        return null;
    }
    
    public String saveChildEntry(Path rootPath, String entryName, Compressed compressed) throws IOException {
        File outputFile = null;
        String patternStr = ".*"+Pattern.quote(File.separator)+"([^"+Pattern.quote(File.separator)+"]+)$";
        String shortFilename = filename.replaceAll(patternStr, "$1");	// ".*\\\\([^\\\\]+)$"
        shortFilename = shortFilename.replaceAll("(.*)\\.hwp$", "$1");
        log.finer("HwpFilePath="+filename + ", FileName="+shortFilename);
        File rootFolder = new File(rootPath.toFile(), shortFilename);
        Path basePath;
        if (!rootFolder.exists()) {
            basePath = Files.createDirectories(Paths.get(rootPath.toString(), shortFilename));
        } else {
            basePath = Paths.get(rootPath.toString(), shortFilename);
        }
        DirectoryEntry targetEntry = null;
        
        List<DirectoryEntry> entries = oleFile.getChildEntries("Root Entry");
        for (DirectoryEntry entry: entries) {
            if (entry.getObjectType()==0x01) {
                Path childPath = Paths.get(rootPath.toString(), shortFilename, entry.getDirectoryEntryName().trim());
                basePath = childPath;
                if (!childPath.toFile().exists()) {
                    Files.createDirectory(childPath);
                }
                targetEntry = searchChildEntry(childPath, entry, entryName);
                if (targetEntry!=null)	break;
            } else {
                if (entry.getDirectoryEntryName().trim().equals(entryName)) {
                    targetEntry = entry;
                    break;
                }
            }
        }
        if (targetEntry != null) {
            outputFile = Paths.get(basePath.toString(), targetEntry.getDirectoryEntryName().trim()).toFile();
            if (outputFile.exists()==false) {
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    if (compressed == Compressed.COMPRESS || (compressed==Compressed.FOLLOW_STORAGE && fileHeader.bCompressed)) {
                        fos.write(unzip(oleFile.read(targetEntry)));
                    } else {
                        fos.write(oleFile.read(targetEntry));
                    }
                } catch (DataFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return outputFile==null?null:outputFile.getAbsolutePath();
    }
    
    public byte[] getChildBytes(String entryName, Compressed compressed) throws IOException {
        byte[] retBytes = null;
        String patternStr = ".*"+Pattern.quote(File.separator)+"([^"+Pattern.quote(File.separator)+"]+)$";
        String shortFilename = filename.replaceAll(patternStr, "$1");	// ".*\\\\([^\\\\]+)$"		// 불필요코드이나 현재는 유지
        shortFilename = shortFilename.replaceAll("(.*)\\.hwp$", "$1");								// 불필요코드이나 현재는 유지
        log.finer("HwpFilePath="+filename + ", FileName="+shortFilename);
        DirectoryEntry targetEntry = null;
        
        List<DirectoryEntry> entries = oleFile.getChildEntries("Root Entry");
        for (DirectoryEntry entry: entries) {
            if (entry.getObjectType()==0x01) {
                Path childPath = Paths.get(shortFilename, entry.getDirectoryEntryName().trim());	// 불필요코드이나 현재는 유지.
                targetEntry = searchChildEntry(childPath, entry, entryName);
                if (targetEntry!=null)	break;
            } else {
                if (entry.getDirectoryEntryName().trim().equals(entryName)) {
                    targetEntry = entry;
                    break;
                }
            }
        }
        if (targetEntry != null) {
            if (compressed == Compressed.COMPRESS || (compressed==Compressed.FOLLOW_STORAGE && fileHeader.bCompressed)) {
                try {
                    retBytes = unzip(oleFile.read(targetEntry));
                } catch (IOException | DataFormatException e) {
                    e.printStackTrace();
                }
            } else {
                retBytes = oleFile.read(targetEntry);
            }
        }
        
        return retBytes;
    }
    
	private byte[] unzip(byte[] input) throws IOException, DataFormatException {
        Inflater decompressor = new Inflater(true);
        decompressor.setInput(input, 0, input.length);
        
        byte[] retBytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length)) {
            // Decompress the data
            byte[] buf = new byte[8096];
            while (!decompressor.finished()) {
                int count = decompressor.inflate(buf);
                if (count > 0) {
                    bos.write(buf, 0, count);
                } else {
                    throw new IOException("can't decompress data");
                }
            }
            bos.close();
            retBytes = bos.toByteArray();
        }
        return retBytes;
    }
	
    private byte[] decrypt(byte[] buf) throws HwpParseException {
        int offset 	= 0;
        int header 	= buf[offset+3]<<24&0xFF000000 | buf[offset+2]<<16&0xFF0000 | buf[offset+1]<<8&0xFF00 | buf[offset]&0xFF;
        int tagNum 	= header&0x3FF;				// 10 bits (0 - 9 bit)
        int level 	= (header&0xFFC00)>>>10;	// 10 bits (10-19 bit)
        int size 	= (header&0xFFF00000)>>>20;	// 12 bits (20-31 bit)
        offset += 4;
        
        HwpTag tag = HwpTag.from(tagNum);
        if (tag!=HwpTag.HWPTAG_DISTRIBUTE_DOC_DATA) {
            throw new HwpParseException();
        }
        if (size != 256) {
            throw new HwpParseException();
        }
        
        byte[] docData = new byte[256];
        System.arraycopy(buf, offset, docData, 0, 256);
        offset += 256;
        
        int seed = docData[3]<<24&0xFF000000 | docData[2]<<16&0xFF0000 | docData[1]<<8&0xFF00 | docData[0]&0xFF;
        int hashoffset = (seed & 0x0f)+4;
        
        // 한글문서파일형식_배포용문서_revision1.2.hwp
        // MS Visual C의 랜덤함수 srand(), rand()를 사용
        // 1. srand() 초기화 (Seed 사용)
        // 2. rand()함수의 결과 값을 이용하여 배열을 채운다. 단, rand()함수가 호출되는 순번에 따라 그 사용 방식이 달라진다.홀수번째 : 배열에 채워지는 값짝수번째 : 배열에 채워지는 횟수
        // 3. 홀수번째 rand() & 0xFF의 값을 A라 하고, 짝수번째 (rand() & 0x0F + 1)의 결과를 B라 할 때배열에 A값을 B번 횟수만큼 삽입한다.예를 들어 A가 ‘a’이고, B가 3일 경우에 배열에 ‘a’를 3번 삽입한다.
        // 4. 배열크기가 256이 될 때까지 3항을 반복한다.
        Rand.srand(seed);
        for (int i=0; i < 256; ) {
            byte a = (byte) (Rand.rand() & 0x000000FF);
            int cnt = (Rand.rand() & 0x0000000F) + 1;
            for (int j=0; j<cnt && i<256; j++,i++) {
                docData[i] ^= a;
            }
        }
        
        byte[] output = null;
        try {
            SecretKeySpec skey = new SecretKeySpec(docData, hashoffset, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skey);
            output = cipher.doFinal(buf, offset, buf.length-offset);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        
        return output;
    }
    
    public boolean getFileHeader() throws CompoundDetectException, HwpDetectException {
        return fileHeader.parse(getComponent("FileHeader"));
    }
    
    public HwpDocInfo getDocInfo() {
        return docInfo;
    }
    
    public boolean getDocInfo(int version) throws CompoundDetectException, IOException, DataFormatException, HwpParseException, NotImplementedException {
        if (fileHeader.bCompressed)
            return docInfo.parse(unzip(getComponent("DocInfo")), version);
        else 
            return docInfo.parse(getComponent("DocInfo"), version);
    }
    
    private boolean getBodyText(int version) throws HwpParseException, IOException, DataFormatException {
        List<DirectoryEntry> sections = oleFile.getChildEntries("BodyText");
        log.fine("BodyText has " + sections.size() + " children");
        for (DirectoryEntry section: sections) {
            HwpSection hwpSection = new HwpSection(this);
            if (fileHeader.bCompressed) {
                hwpSection.parse(unzip(oleFile.read(section)), version);
            } else {
                hwpSection.parse(oleFile.read(section), version);
            }
            bodyText.add(hwpSection);
        }
        return true;
    }
    
    private boolean getViewText(int version) throws HwpParseException, NotImplementedException, IOException, DataFormatException {
        List<DirectoryEntry> sections = oleFile.getChildEntries("ViewText");
        log.fine("ViewText has " + sections.size() + " children");
        for (DirectoryEntry section: sections) {
            HwpSection hwpSection = new HwpSection(this);
            if (fileHeader.bCompressed) {
                hwpSection.parse(unzip(decrypt(oleFile.read(section))), version);
            } else {
                hwpSection.parse(decrypt(oleFile.read(section)), version);
            }
            viewText.add(hwpSection);
        }
        return true;
    }
    
    public byte[] getComponent(String entryName) throws CompoundDetectException {
        return oleFile.getComponent(entryName);
    }
    
    public void close() throws IOException {
        oleFile.close();
    }
    
    public List<DirectoryEntry> getBinData() {
        return directoryBinData;
    }
    
    public void setBinData(List<DirectoryEntry> binData) {
        this.directoryBinData = binData;
    }
    
    public List<HwpParagraph> getParaList() {
        return paraList;
    }
    
    public void addParaList(HwpParagraph para) {
        if (this.paraList == null) 
            this.paraList = new ArrayList<HwpParagraph>();
        this.paraList.add(para);
    }
    
    public static class Rand {
        static int random_seed;
        
        public static void srand(int seed) {
            random_seed = seed;
        }
        
        public static int rand() {
            random_seed = (random_seed * 214013 + 2531011) & 0xFFFFFFFF;
            return ((random_seed >> 16) & 0x7FFF);
        }
    }
} 
