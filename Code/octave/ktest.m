function [x_n, P_n] = ktest(x, z, phi, H, P, Q, R)
	
	K = P * H' * (inv(H * P * H' + R));
	
	x_n = phi * x + K * (z - H * phi * x);
	
	P_n = (eye(size(P,1)) - K * H) * P * ((eye(size(P,1)) - K * H))' + K * R * K';
	
	P_n = phi * P_n * phi' + Q;
	
	%disp(x);
	%disp(z);
	%disp(phi);
	%disp(H);
	%disp(P);
	%disp(Q);
	%disp(R);