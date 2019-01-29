#ifndef LEELAZERO_H
#define LEELAZERO_H
//extern "C" {
int StartEngine(std::string weightfilename);
int SendGTP(std::string cmd);
std::string getStdout();
//};
#endif