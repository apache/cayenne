CREATE OR REPLACE FUNCTION cayenne_tst_select_proc (a_name IN VARCHAR2, painting_price IN NUMBER)
    RETURN cayenne_types.ref_cursor
AS
   artists cayenne_types.ref_cursor;
BEGIN
      SET TRANSACTION READ WRITE;
      UPDATE PAINTING SET ESTIMATED_PRICE = ESTIMATED_PRICE * 2
      WHERE ESTIMATED_PRICE < painting_price;
      COMMIT;
 
     OPEN artists FOR
     SELECT DISTINCT A.ARTIST_ID, A.ARTIST_NAME, A.DATE_OF_BIRTH
     FROM ARTIST A, PAINTING P
     WHERE A.ARTIST_ID = P.ARTIST_ID AND
     RTRIM(A.ARTIST_NAME) = a_name
     ORDER BY A.ARTIST_ID;

     RETURN artists;
END;

