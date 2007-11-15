CREATE PROCEDURE cayenne_tst_out_proc (IN p1 INT, OUT p2 INT) 
BEGIN
    SELECT p1 * 2 INTO p2;
END
