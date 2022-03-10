package core

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"io"
	"io/ioutil"
	"strings"

	ipfs_api "github.com/ipfs/go-ipfs-api"
	files "github.com/ipfs/go-ipfs-files"
)

type object struct {
	Hash string
}

type Shell struct {
	ishell *ipfs_api.Shell
	url    string
}

func NewShell(url string) *Shell {
	return &Shell{
		ishell: ipfs_api.NewShell(url),
		url:    url,
	}
}

func (s *Shell) NewRequest(command string) *RequestBuilder {
	return &RequestBuilder{
		rb: s.ishell.Request(strings.TrimLeft(command, "/")),
	}
}

func (s *Shell) Add(data []byte) (string, error) {
	return s.ishell.Add(bytes.NewReader(data))
}

//  -w, --wrap-with-directory  bool   - Wrap files with a directory object.
func (s *Shell) AddW(data []byte, name string) (string, error) {
	fr := files.NewReaderFile(bytes.NewReader(data))
	slf := files.NewSliceDirectory([]files.DirEntry{files.FileEntry(name, fr)})
	fileReader := files.NewMultiFileReader(slf, true)

	resp, err := s.ishell.Request("add").
		Option("wrap-with-directory", true).
		Body(fileReader).
		Send(context.Background())
	if err != nil {
		return "", err
	}

	defer resp.Close()

	if resp.Error != nil {
		return "", resp.Error
	}

	dec := json.NewDecoder(resp.Output)
	var final string
	for {
		var out object
		err = dec.Decode(&out)
		if err != nil {
			if err == io.EOF {
				break
			}
			return "", err
		}
		final = out.Hash
	}

	if final == "" {
		return "", errors.New("no results received")
	}

	return final, nil
}

type SubListener interface {
	OnListener(data string)
}

func (s *Shell) PubSubSubscribe(topic string, subListener SubListener) {
	go func() {
		pss, err := s.ishell.PubSubSubscribe(topic)
		if err != nil {
			subListener.OnListener(err.Error())
			return
		}

		for {
			msg, err := pss.Next()
			if err != nil {
				subListener.OnListener(err.Error())
				continue
			}
			subListener.OnListener(string(msg.Data))
		}
	}()
}

type RequestBuilder struct {
	rb *ipfs_api.RequestBuilder
}

func (req *RequestBuilder) Send() ([]byte, error) {
	res, err := req.rb.Send(context.Background())
	if err != nil {
		return nil, err
	}

	defer res.Close()
	if res.Error != nil {
		return nil, res.Error
	}

	return ioutil.ReadAll(res.Output)
}

// func (req *RequestBuilder) Add(data []byte) (string, error) {
// 	fr := files.NewReaderFile(bytes.NewReader(data))
// 	slf := files.NewSliceDirectory([]files.DirEntry{files.FileEntry("", fr)})
// 	fileReader := files.NewMultiFileReader(slf, true)

// 	req.rb.Body(fileReader)
// 	type object struct {
// 		Hash string
// 	}
// 	var out object
// 	err := req.rb.Exec(context.Background(), &out)
// 	if err != nil {
// 		return "", err
// 	}
// 	return out.Hash, nil
// }

func (req *RequestBuilder) Argument(arg string) {
	req.rb.Arguments(arg)
}

func (req *RequestBuilder) BoolOptions(key string, value bool) {
	req.rb.Option(key, value)
}

func (req *RequestBuilder) ByteOptions(key string, value []byte) {
	req.rb.Option(key, value)
}

func (req *RequestBuilder) StringOptions(key string, value string) {
	req.rb.Option(key, value)
}

func (req *RequestBuilder) BodyString(body string) {
	req.rb.BodyString(body)
}

func (req *RequestBuilder) BodyBytes(body []byte) {
	req.rb.BodyBytes(body)
}

func (req *RequestBuilder) Header(name, value string) {
	req.rb.Header(name, value)
}

// Helpers

// New unix socket domain shell
func NewUDSShell(sockpath string) *Shell {
	return NewShell("/unix/" + sockpath)
}

func NewTCPShell(port string) *Shell {
	return NewShell("/ip4/127.0.0.1/tcp/" + port)
}
