package service.vcat.smartro.com.vcat;

/**
*  This interface class is used to make your application.
*  Please do not modify below codes if you do not want to get errors on your application.
*
*  이 인터페이스 클래스는 어플케이션 제작에 도움을 주기 위해 만들어졌습니다.
*  오류 발생을 막기 위해, 아래 코드는 수정하지 마시길 바랍니다.
*
*  ----------------------------------------------------------------------------------------
*  최종 수정일 : 2019/01/09
*  작업자 : JINKYU LEE (jglee@smartro.co.kr)
*  ----------------------------------------------------------------------------------------
**/
import service.vcat.smartro.com.vcat.SmartroVCatCallback;

interface SmartroVCatInterface
{
    void executeService(in String strJSON, in SmartroVCatCallback svcbPoint);
    void postExtraData(in String strJSON);
    void cancelService();
}